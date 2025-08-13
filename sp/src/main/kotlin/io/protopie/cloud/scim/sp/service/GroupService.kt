package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.protopie.cloud.scim.sp.database.Groups
import io.protopie.cloud.scim.sp.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
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

    /**
     * ResultRow를 Group 객체로 변환
     */
    private fun resultRowToGroup(row: org.jetbrains.exposed.sql.ResultRow): Group {
        val id = row[Groups.id].value.toString()
        val membersJson = row[Groups.membersJson]
        val metaJson = row[Groups.metaJson]

        // JSON 문자열을 객체로 변환
        val members = membersJson?.let { objectMapper.readValue(it, Array<Member>::class.java).toList() }
        val meta = metaJson?.let { objectMapper.readValue(it, Meta::class.java) }

        return Group(
            id = id,
            externalId = row[Groups.externalId],
            displayName = row[Groups.displayName],
            members = members,
            meta = meta,
        )
    }

    /**
     * 그룹 목록 조회
     * @param startIndex 시작 인덱스 (1부터 시작)
     * @param count 조회할 아이템 수
     * @return 그룹 목록 응답
     */
    fun getGroups(
        startIndex: Int = 1,
        count: Int = 100,
    ): GroupListResponse =
        transaction {
            val actualStartIndex = if (startIndex < 1) 1 else startIndex
            val limit = count
            val offset = (actualStartIndex - 1).toLong()

            // 전체 그룹 수 조회
            val totalCount =
                Groups
                    .selectAll()
                    .count()

            // 페이지네이션 적용한 그룹 목록 조회
            val groupsList =
                Groups
                    .selectAll()
                    .orderBy(Groups.displayName to org.jetbrains.exposed.sql.SortOrder.ASC)
                    .limit(limit, offset)
                    .map { resultRow -> resultRowToGroup(resultRow) }

            GroupListResponse(
                totalResults = totalCount.toInt(),
                startIndex = actualStartIndex,
                itemsPerPage = groupsList.size,
                resources = groupsList,
            )
        }

    /**
     * 특정 그룹 조회
     */
    fun getGroupById(groupId: String): Group? =
        transaction {
            try {
                val uuid = UUID.fromString(groupId)
                Groups
                    .select { Groups.id eq uuid }
                    .singleOrNull()
                    ?.let { resultRow -> resultRowToGroup(resultRow) }
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $groupId")
                null
            }
        }

    /**
     * 그룹 생성
     */
    fun createGroup(group: Group): Group =
        transaction {
            // displayName으로 중복 체크
            val existingGroup =
                Groups
                    .select {
                        Groups.displayName eq
                            group.displayName
                    }.singleOrNull()
            if (existingGroup != null) {
                throw IllegalStateException("Group with displayName ${group.displayName} already exists")
            }

            val groupId = group.id?.let { UUID.fromString(it) } ?: UUID.randomUUID()
            val now = java.time.LocalDateTime.now()

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

            // 메타데이터 생성
            val meta =
                Meta(
                    resourceType = "Group",
                    created = now,
                    lastModified = now,
                    location = "/Groups/$groupId",
                )

            // 복잡한 필드를 JSON으로 변환
            val membersJson = updatedMembers?.let { objectMapper.writeValueAsString(it) }
            val metaJson = objectMapper.writeValueAsString(meta)

            // 데이터베이스에 삽입
            Groups.insert { row ->
                row[id] = groupId
                row[externalId] = group.externalId
                row[displayName] = group.displayName
                row[created] = now
                row[lastModified] = now
                row[Groups.membersJson] = membersJson
                row[Groups.metaJson] = metaJson
            }

            // 생성된 그룹 반환
            group.copy(
                id = groupId.toString(),
                members = updatedMembers,
                meta = meta,
            )
        }

    /**
     * 그룹 전체 수정
     */
    fun updateGroup(
        groupId: String,
        group: Group,
    ): Group? =
        transaction {
            try {
                val uuid = UUID.fromString(groupId)
                val existingGroup =
                    Groups
                        .select { Groups.id eq uuid }
                        .singleOrNull()
                        ?: return@transaction null

                val now = java.time.LocalDateTime.now()
                val createdDate = existingGroup[Groups.created]

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

                // 메타데이터 업데이트
                val meta =
                    Meta(
                        resourceType = "Group",
                        created = createdDate,
                        lastModified = now,
                        location = "/Groups/$groupId",
                    )

                // 복잡한 필드를 JSON으로 변환
                val membersJson = updatedMembers?.let { objectMapper.writeValueAsString(it) }
                val metaJson = objectMapper.writeValueAsString(meta)

                // 데이터베이스 업데이트
                Groups.update({ Groups.id eq uuid }) { row ->
                    row[externalId] = group.externalId
                    row[displayName] = group.displayName
                    row[lastModified] = now
                    row[Groups.membersJson] = membersJson
                    row[Groups.metaJson] = metaJson
                }

                // 업데이트된 그룹 반환
                group.copy(
                    id = groupId,
                    members = updatedMembers,
                    meta = meta,
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $groupId")
                null
            }
        }

    /**
     * 그룹 부분 수정 (패치)
     */
    fun patchGroup(
        groupId: String,
        patchOp: PatchOp,
    ): Group? =
        transaction {
            try {
                val uuid = UUID.fromString(groupId)
                val existingGroupRow =
                    Groups
                        .select { Groups.id eq uuid }
                        .singleOrNull()
                        ?: return@transaction null

                // 기존 그룹 데이터를 객체로 변환
                var patchedGroup = resultRowToGroup(existingGroupRow)

                logger.info("Received patch operation for group $groupId: $patchOp")
                logger.info(
                    "Patching group $groupId with operations: ${patchOp.operations.map {
                        "${it.op}${it.path?.let { p ->
                            " path=$p"
                        } ?: ""}"
                    }}",
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
                val now = java.time.LocalDateTime.now()
                val created = existingGroupRow[Groups.created]
                val meta =
                    Meta(
                        resourceType = "Group",
                        created = created,
                        lastModified = now,
                        location = "/Groups/$groupId",
                    )

                // 복잡한 필드를 JSON으로 변환
                val membersJson = patchedGroup.members?.let { objectMapper.writeValueAsString(it) }
                val metaJson = objectMapper.writeValueAsString(meta)

                // 데이터베이스 업데이트
                Groups.update({ Groups.id eq uuid }) { row ->
                    row[externalId] = patchedGroup.externalId
                    row[displayName] = patchedGroup.displayName
                    row[lastModified] = now
                    row[Groups.membersJson] = membersJson
                    row[Groups.metaJson] = metaJson
                }

                // 업데이트된 그룹 반환
                patchedGroup.copy(meta = meta)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $groupId")
                null
            }
        }

    /**
     * 그룹 삭제
     */
    fun deleteGroup(groupId: String): Boolean =
        transaction {
            try {
                val uuid = UUID.fromString(groupId)
                val deletedRowCount =
                    Groups.deleteWhere {
                        Groups.id eq
                            uuid
                    }
                deletedRowCount > 0
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $groupId")
                false
            }
        }

    /**
     * 모든 그룹 데이터 초기화 (테스트용)
     */
    fun clearAllGroups() =
        transaction {
            logger.info("Clearing all groups from database")
            val countBefore =
                Groups
                    .selectAll()
                    .count()
            Groups
                .deleteAll()
            logger.info("All groups cleared - count before: $countBefore, after: 0")
        }
}
