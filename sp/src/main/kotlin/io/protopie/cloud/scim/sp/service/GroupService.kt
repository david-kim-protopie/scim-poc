package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.protopie.cloud.scim.sp.models.*
import java.time.LocalDateTime
import java.util.*

/**
 * Group 리소스를 관리하는 서비스
 */
class GroupService(
    private val userService: UserService,
) {
    private val objectMapper = jacksonObjectMapper()

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
        val group = groups[groupId] ?: return null
        var patchedGroup = group

        for (operation in patchOp.operations) {
            when (operation.op.lowercase()) {
                "add" -> {
                    if (operation.path == "members") {
                        // 멤버 추가
                        val newMembers = ArrayList(patchedGroup.members ?: listOf())
                        val valueNode = operation.value

                        if (valueNode != null) {
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
                                val member = objectMapper.treeToValue(valueNode, Member::class.java)
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
                        }

                        patchedGroup = patchedGroup.copy(members = newMembers)
                    }
                }
                "replace" -> {
                    if (operation.path == "displayName" && operation.value != null) {
                        patchedGroup = patchedGroup.copy(displayName = operation.value.asText())
                    } else if (operation.path == "members" && operation.value != null) {
                        val newMembers = mutableListOf<Member>()
                        if (operation.value.isArray) {
                            operation.value.forEach { memberNode ->
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
                "remove" -> {
                    if (operation.path == "members" && operation.value != null) {
                        val membersToRemove = mutableListOf<String>()
                        if (operation.value.isArray) {
                            operation.value.forEach { memberNode ->
                                if (memberNode.has("value")) {
                                    membersToRemove.add(memberNode.get("value").asText())
                                }
                            }
                        } else if (operation.value.has("value")) {
                            membersToRemove.add(operation.value.get("value").asText())
                        }

                        val newMembers =
                            patchedGroup.members?.filter { member ->
                                !membersToRemove.contains(member.value)
                            } ?: listOf()

                        patchedGroup = patchedGroup.copy(members = newMembers)
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
}
