package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.protopie.cloud.scim.sp.models.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * User 리소스를 관리하는 서비스
 */
class UserService {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    private val objectMapper = jacksonObjectMapper()

    // 임시 메모리 저장소
    private val users = mutableMapOf<String, User>()

    /**
     * 사용자 목록 조회
     * @param startIndex 시작 인덱스 (1부터 시작)
     * @param count 조회할 아이템 수
     * @return 사용자 목록 응답
     */
    fun getUsers(
        startIndex: Int = 1,
        count: Int = 100,
    ): UserListResponse {
        val allUsers = users.values.toList()
        val actualStartIndex = if (startIndex < 1) 1 else startIndex
        val endIndex = (actualStartIndex - 1 + count).coerceAtMost(allUsers.size)
        val startPos = (actualStartIndex - 1).coerceAtMost(allUsers.size)

        val paginatedUsers =
            if (startPos < allUsers.size) {
                allUsers.subList(startPos, endIndex)
            } else {
                emptyList()
            }

        return UserListResponse(
            totalResults = allUsers.size,
            startIndex = actualStartIndex,
            itemsPerPage = paginatedUsers.size,
            resources = paginatedUsers,
        )
    }

    /**
     * 특정 사용자 조회
     */
    fun getUserById(userId: String): User? = users[userId]

    /**
     * 사용자 생성
     */
    fun createUser(user: User): User {
        // userName으로 중복 체크
        if (users.values.any { it.userName == user.userName }) {
            throw IllegalStateException("User with userName ${user.userName} already exists")
        }

        val userId = user.id ?: UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        val newUser =
            user.copy(
                id = userId,
                externalId = user.externalId,
                meta =
                    Meta(
                        resourceType = "User",
                        created = now,
                        lastModified = now,
                        location = "/Users/${user.externalId}",
                    ),
            )

        users[userId] = newUser
        return newUser
    }

    /**
     * 사용자 수정 (전체 교체)
     */
    fun updateUser(
        externalId: String,
        user: User,
    ): User? {
        if (!users.containsKey(externalId)) return null

        val now = LocalDateTime.now()
        val meta = users[externalId]?.meta

        val updatedUser =
            user.copy(
                id = externalId,
                meta =
                    meta?.copy(
                        lastModified = now,
                    ) ?: Meta(
                        resourceType = "User",
                        created = now,
                        lastModified = now,
                        location = "/Users/$externalId",
                    ),
            )

        users[externalId] = updatedUser
        return updatedUser
    }

    /**
     * 사용자 부분 수정 (패치)
     */
    fun patchUser(
        externalId: String,
        patchOp: PatchOp,
    ): User? {
        val user = users[externalId] ?: return null
        var patchedUser = user

        for (operation in patchOp.operations) {
            when (operation.op.lowercase()) {
                "add" -> {
                    // Do Nothing
                }
                "replace" -> {
                    operation.value?.let { textNode ->
                        val rootNode = objectMapper.readTree(textNode.asText())
                        // name 필드 업데이트 처리
                        if (rootNode.has("name")) {
                            logger.info("Updating name field")
                            val nameNode = rootNode.get("name")
                            // 기존 이름 정보를 유지하면서 새 필드 업데이트
                            val currentName = patchedUser.name ?: Name()
                            val updatedName = objectMapper.treeToValue(nameNode, Name::class.java)

                            // null이 아닌 필드만 업데이트
                            val mergedName =
                                currentName.copy(
                                    formatted = updatedName.formatted ?: currentName.formatted,
                                    familyName = updatedName.familyName ?: currentName.familyName,
                                    givenName = updatedName.givenName ?: currentName.givenName,
                                    middleName = updatedName.middleName ?: currentName.middleName,
                                    honorificPrefix = updatedName.honorificPrefix ?: currentName.honorificPrefix,
                                    honorificSuffix = updatedName.honorificSuffix ?: currentName.honorificSuffix,
                                )

                            patchedUser = patchedUser.copy(name = mergedName)
                        }

                        // emails 필드 업데이트 처리
                        if (rootNode.has("emails")) {
                            logger.info("Updating emails field")
                            val emailsNode = rootNode.get("emails")
                            val emails = mutableListOf<ComplexAttribute>()
                            if (emailsNode.isArray) {
                                emailsNode.forEach { emailNode ->
                                    val email = objectMapper.treeToValue(emailNode, ComplexAttribute::class.java)
                                    emails.add(email)
                                }
                            }
                            patchedUser = patchedUser.copy(emails = emails)
                        }

                        // active 필드 업데이트 처리
                        if (rootNode.has("active")) {
                            logger.info("Updating active field to: ${rootNode.get("active").asBoolean()}")
                            patchedUser = patchedUser.copy(active = rootNode.get("active").asBoolean())
                        }

                        // userName 필드 업데이트 처리
                        if (rootNode.has("userName")) {
                            logger.info("Updating userName field to: ${rootNode.get("userName").asText()}")
                            patchedUser = patchedUser.copy(userName = rootNode.get("userName").asText())
                        }

                        // displayName 필드 업데이트 처리
                        if (rootNode.has("displayName")) {
                            logger.info("Updating displayName field to: ${rootNode.get("displayName").asText()}")
                            patchedUser = patchedUser.copy(displayName = rootNode.get("displayName").asText())
                        }

                        // externalId 필드 업데이트 처리
                        if (rootNode.has("externalId")) {
                            logger.info("Updating externalId field to: ${rootNode.get("externalId").asText()}")
                            patchedUser = patchedUser.copy(externalId = rootNode.get("externalId").asText())
                        }
                    }
                }
                "remove" -> {
                    // Do Nothing
                }
            }
        }

        // 메타데이터 업데이트
        val now = LocalDateTime.now()
        patchedUser =
            patchedUser.copy(
                meta =
                    patchedUser.meta?.copy(
                        lastModified = now,
                    ) ?: Meta(
                        resourceType = "User",
                        created = now,
                        lastModified = now,
                        location = "/Users/$externalId",
                    ),
            )

        users[externalId] = patchedUser
        return patchedUser
    }

    /**
     * 사용자 삭제
     */
    fun deleteUser(externalId: String): Boolean = users.remove(externalId) != null
}
