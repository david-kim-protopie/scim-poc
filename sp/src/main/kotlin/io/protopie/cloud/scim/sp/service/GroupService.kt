package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.protopie.cloud.scim.sp.models.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * Group 리소스를 관리하는 서비스
 */
class GroupService(
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(GroupService::class.java)
    private val objectMapper = jacksonObjectMapper()

    /**
     * members[value eq "uuid"] 형식의 경로에서 필터 추출
     * @return 필터에 지정된 value 값 또는 null
     */
    private fun parseMemberValueFilter(path: String): String? {
        // members[value eq "uuid"] 패턴 확인
        val pattern = """members\[value eq "([a-z0-9-]+)"\]"""
        val regex = Regex(pattern)
        val matchResult = regex.find(path)
        return matchResult?.groups?.get(1)?.value
    }

    // 임시 메모리 저장소
    private val groups = mutableMapOf<String, Group>()

    /**
     * 그룹 목록 조회
     * @param startIndex 시작 인덱스 (1부터 시작)
     * @param count 조회할 아이템 수
     * @return 그룹 목록 응답
     */
    fun getGroups(
        startIndex: Int = 1,
        count: Int = 100,
    ): GroupListResponse {
        val allGroups = groups.values.toList()
        val actualStartIndex = if (startIndex < 1) 1 else startIndex
        val endIndex = (actualStartIndex - 1 + count).coerceAtMost(allGroups.size)
        val startPos = (actualStartIndex - 1).coerceAtMost(allGroups.size)

        val paginatedGroups =
            if (startPos < allGroups.size) {
                allGroups.subList(startPos, endIndex)
            } else {
                emptyList()
            }

        return GroupListResponse(
            totalResults = allGroups.size,
            startIndex = actualStartIndex,
            itemsPerPage = paginatedGroups.size,
            resources = paginatedGroups,
        )
    }

    /**
     * 특정 그룹 조회
     */
    fun getGroupById(groupId: String): Group? = groups[groupId]

    /**
     * 그룹 생성
     */
    fun createGroup(group: Group): Group {
        // displayName으로 중복 체크
        if (groups.values.any { it.displayName == group.displayName }) {
            throw IllegalStateException("Group with displayName ${group.displayName} already exists")
        }

        val groupId = group.id ?: UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        // 멤버 정보 보강
        val updatedMembers =
            group.members?.map { member ->
                val user = userService.getUserById(member.value)
                if (user != null && member.display == null) {
                    member.copy(display = user.userName)
                } else {
                    member
                }
            }

        val newGroup =
            group.copy(
                id = groupId,
                externalId = group.externalId,
                members = updatedMembers,
                meta =
                    Meta(
                        resourceType = "Group",
                        created = now,
                        lastModified = now,
                        location = "/Groups/$groupId",
                    ),
            )

        groups[groupId] = newGroup
        return newGroup
    }

    /**
     * 그룹 전체 수정
     */
    fun updateGroup(
        groupId: String,
        group: Group,
    ): Group? {
        if (!groups.containsKey(groupId)) return null

        val now = LocalDateTime.now()
        val meta = groups[groupId]?.meta

        // 멤버 정보 보강
        val updatedMembers =
            group.members?.map { member ->
                val user = userService.getUserById(member.value)
                if (user != null && member.display == null) {
                    member.copy(display = user.userName)
                } else {
                    member
                }
            }

        val updatedGroup =
            group.copy(
                id = groupId,
                members = updatedMembers,
                meta =
                    meta?.copy(
                        lastModified = now,
                    ) ?: Meta(
                        resourceType = "Group",
                        created = now,
                        lastModified = now,
                        location = "/Groups/$groupId",
                    ),
            )

        groups[groupId] = updatedGroup
        return updatedGroup
    }

    /**
     * 그룹 부분 수정 (패치)
     */
    fun patchGroup(
        groupId: String,
        patchOp: PatchOp,
    ): Group? {
        logger.info("Received patch operation for group $groupId: $patchOp")
        val group = groups[groupId] ?: return null
        var patchedGroup = group

        logger.info(
            "Patching group $groupId with operations: ${patchOp.operations.map { "${it.op}${it.path?.let { p -> " path=$p" } ?: ""}" }}",
        )

        for (operation in patchOp.operations) {
            when (operation.op.lowercase()) {
                "add" -> {
                    if (operation.path == "members") {
                        // 멤버 추가
                        val newMembers = ArrayList(patchedGroup.members ?: listOf())
                        val valueNode = operation.value

                        if (valueNode != null) {
                            try {
                                logger.info("Adding member with value node: $valueNode")
                                if (valueNode.isArray) {
                                    valueNode.forEach { memberNode ->
                                        val member = objectMapper.treeToValue(memberNode, Member::class.java)
                                        if (!newMembers.any { it.value == member.value }) {
                                            // 멤버 정보 보강
                                            val user = userService.getUserById(member.value)
                                            val updatedMember =
                                                if (user != null && member.display == null) {
                                                    member.copy(display = user.userName)
                                                } else {
                                                    member
                                                }
                                            newMembers.add(updatedMember)
                                        }
                                    }
                                } else {
                                    // 직접 Member 객체로 변환 시도
                                    val member = objectMapper.treeToValue(valueNode, Member::class.java)
                                    logger.info("Parsed member: $member")
                                    if (member != null && !newMembers.any { it.value == member.value }) {
                                        // 멤버 정보 보강
                                        val user = userService.getUserById(member.value)
                                        val updatedMember =
                                            if (user != null && member.display == null) {
                                                member.copy(display = user.userName)
                                            } else {
                                                member
                                            }
                                        newMembers.add(updatedMember)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Error processing member value: $valueNode", e)
                                // 실패한 경우 대체 방법으로 시도
                                try {
                                    val valueStr = valueNode.toString()
                                    logger.info("Trying alternate parsing method with value: $valueStr")
                                    val member = objectMapper.readValue(valueStr, Member::class.java)
                                    if (!newMembers.any { it.value == member.value }) {
                                        val user = userService.getUserById(member.value)
                                        val updatedMember =
                                            if (user != null && member.display == null) {
                                                member.copy(display = user.userName)
                                            } else {
                                                member
                                            }
                                        newMembers.add(updatedMember)
                                    }
                                } catch (e2: Exception) {
                                    logger.error("Failed alternate parsing method", e2)
                                }
                            }
                        }
                        patchedGroup = patchedGroup.copy(members = newMembers)
                    }
                }
                "replace" -> {
                    operation.value?.let { textNode ->
                        val rootNode = objectMapper.readTree(textNode.asText())
                        if (rootNode.has("displayName")) {
                            patchedGroup = patchedGroup.copy(displayName = rootNode.get("displayName").asText())
                        }
                        if (rootNode.has("members")) {
                            val newMembers = mutableListOf<Member>()
                            if (rootNode.isArray) {
                                rootNode.forEach { memberNode ->
                                    val member = objectMapper.treeToValue(memberNode, Member::class.java)
                                    // 멤버 정보 보강
                                    val user = userService.getUserById(member.value)
                                    val updatedMember =
                                        if (user != null && member.display == null) {
                                            member.copy(display = user.userName)
                                        } else {
                                            member
                                        }
                                    newMembers.add(updatedMember)
                                }
                            }
                            patchedGroup = patchedGroup.copy(members = newMembers)
                        }
                    }
                }
                "remove" -> {
                    // 필터 기반 경로 처리 (members[value eq "uuid"] 형식)
                    if (operation.path != null && operation.path.startsWith("members[")) {
                        logger.info("Processing filter-based path: ${operation.path}")
                        val memberIdToRemove = parseMemberValueFilter(operation.path)
                        if (memberIdToRemove != null) {
                            logger.info("Removing member with ID: $memberIdToRemove")
                            // 특정 멤버 하나만 제거
                            val newMembers =
                                patchedGroup.members?.filter { member ->
                                    member.value != memberIdToRemove
                                } ?: listOf()

                            patchedGroup = patchedGroup.copy(members = newMembers)
                        }
                    }
                }
            }
        }

        // 메타데이터 업데이트
        val now = LocalDateTime.now()
        patchedGroup =
            patchedGroup.copy(
                meta =
                    patchedGroup.meta?.copy(
                        lastModified = now,
                    ) ?: Meta(
                        resourceType = "Group",
                        created = now,
                        lastModified = now,
                        location = "/Groups/$groupId",
                    ),
            )

        groups[groupId] = patchedGroup
        return patchedGroup
    }

    /**
     * 그룹 삭제
     */
    fun deleteGroup(groupId: String): Boolean = groups.remove(groupId) != null

    /**
     * 모든 그룹 데이터 초기화 (테스트용)
     */
    fun clearAllGroups() {
        groups.clear()
    }
}
