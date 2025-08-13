package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.protopie.cloud.scim.sp.database.DatabaseFactory
import io.protopie.cloud.scim.sp.database.Users
import io.protopie.cloud.scim.sp.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * User 리소스를 관리하는 서비스
 */
class UserService {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    private val objectMapper = jacksonObjectMapper()

    init {
        // 데이터베이스 초기화
        try {
            DatabaseFactory.init()
        } catch (e: Exception) {
            logger.error("데이터베이스 초기화 실패: ${e.message}")
        }
    }

    /**
     * 사용자 목록 조회
     * @param startIndex 시작 인덱스 (1부터 시작)
     * @param count 조회할 아이템 수
     * @return 사용자 목록 응답
     */
    fun getUsers(
        startIndex: Int = 1,
        count: Int = 100,
    ): UserListResponse =
        transaction {
            val actualStartIndex = if (startIndex < 1) 1 else startIndex
            val limit = count
            val offset = (actualStartIndex - 1).toLong()

            // 전체 사용자 수 조회
            val totalCount = Users.selectAll().count()

            // 페이지네이션 적용한 사용자 목록 조회
            val usersList =
                Users
                    .selectAll()
                    .orderBy(Users.userName to SortOrder.ASC)
                    .limit(limit, offset)
                    .map { resultRow -> resultRowToUser(resultRow) }

            UserListResponse(
                totalResults = totalCount.toInt(),
                startIndex = actualStartIndex,
                itemsPerPage = usersList.size,
                resources = usersList,
            )
        }

    /**
     * ResultRow를 User 객체로 변환
     */
    private fun resultRowToUser(row: ResultRow): User {
        val id = row[Users.id].value.toString()
        val nameJson = row[Users.nameJson]
        val emailsJson = row[Users.emailsJson]
        val metaJson = row[Users.metaJson]

        // JSON 문자열을 객체로 변환
        val name = nameJson?.let { objectMapper.readValue(it, Name::class.java) }
        val emails = emailsJson?.let { objectMapper.readValue(it, object : TypeReference<List<ComplexAttribute>>() {}) }
        val meta = metaJson?.let { objectMapper.readValue(it, Meta::class.java) }

        return User(
            id = id,
            externalId = row[Users.externalId],
            userName = row[Users.userName],
            displayName = row[Users.displayName],
            active = row[Users.active],
            name = name,
            emails = emails,
            meta = meta,
            nickName = row[Users.nickName],
            profileUrl = row[Users.profileUrl],
            title = row[Users.title],
            userType = row[Users.userType],
            preferredLanguage = row[Users.preferredLanguage],
            locale = row[Users.locale],
            timezone = row[Users.timezone],
        )
    }

    /**
     * 특정 사용자 조회
     */
    fun getUserById(userId: String): User? =
        transaction {
            try {
                val uuid = UUID.fromString(userId)
                Users
                    .select { Users.id eq uuid }
                    .singleOrNull()
                    ?.let { resultRow -> resultRowToUser(resultRow) }
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $userId")
                null
            }
        }

    /**
     * 사용자 생성
     */
    fun createUser(user: User): User =
        transaction {
            // userName으로 중복 체크
            val existingUser = Users.select { Users.userName eq user.userName }.singleOrNull()
            if (existingUser != null) {
                throw IllegalStateException("User with userName ${user.userName} already exists")
            }

            val userId = user.id?.let { UUID.fromString(it) } ?: UUID.randomUUID()
            val now = LocalDateTime.now()

            // 메타데이터 생성
            val meta =
                Meta(
                    resourceType = "User",
                    created = now,
                    lastModified = now,
                    location = "/Users/$userId",
                )

            // 복잡한 필드를 JSON으로 변환
            val nameJson = user.name?.let { objectMapper.writeValueAsString(it) }
            val emailsJson = user.emails?.let { objectMapper.writeValueAsString(it) }
            val metaJson = objectMapper.writeValueAsString(meta)

            // 데이터베이스에 삽입
            Users.insert { row ->
                row[id] = userId
                row[externalId] = user.externalId
                row[userName] = user.userName
                row[displayName] = user.displayName
                row[active] = user.active
                row[created] = now
                row[lastModified] = now
                row[Users.nameJson] = nameJson
                row[Users.emailsJson] = emailsJson
                row[Users.metaJson] = metaJson
                row[nickName] = user.nickName
                row[profileUrl] = user.profileUrl
                row[title] = user.title
                row[userType] = user.userType
                row[preferredLanguage] = user.preferredLanguage
                row[locale] = user.locale
                row[timezone] = user.timezone
            }

            // 생성된 사용자 반환
            user.copy(
                id = userId.toString(),
                meta = meta,
            )
        }

    /**
     * 사용자 수정 (전체 교체)
     */
    fun updateUser(
        userId: String,
        user: User,
    ): User? =
        transaction {
            try {
                val uuid = UUID.fromString(userId)
                val existingUser = Users.select { Users.id eq uuid }.singleOrNull() ?: return@transaction null

                val now = LocalDateTime.now()
                val createdDate = existingUser[Users.created]

                // 메타데이터 업데이트
                val meta =
                    Meta(
                        resourceType = "User",
                        created = createdDate,
                        lastModified = now,
                        location = "/Users/$userId",
                    )

                // 복잡한 필드를 JSON으로 변환
                val nameJson = user.name?.let { objectMapper.writeValueAsString(it) }
                val emailsJson = user.emails?.let { objectMapper.writeValueAsString(it) }
                val metaJson = objectMapper.writeValueAsString(meta)

                // 데이터베이스 업데이트
                Users.update({ Users.id eq uuid }) { row ->
                    row[externalId] = user.externalId
                    row[userName] = user.userName
                    row[displayName] = user.displayName
                    row[active] = user.active
                    row[lastModified] = now
                    row[Users.nameJson] = nameJson
                    row[Users.emailsJson] = emailsJson
                    row[Users.metaJson] = metaJson
                    row[nickName] = user.nickName
                    row[profileUrl] = user.profileUrl
                    row[title] = user.title
                    row[userType] = user.userType
                    row[preferredLanguage] = user.preferredLanguage
                    row[locale] = user.locale
                    row[timezone] = user.timezone
                }

                // 업데이트된 사용자 반환
                user.copy(
                    id = userId,
                    meta = meta,
                )
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $userId")
                null
            }
        }

    /**
     * 사용자 부분 수정 (패치)
     */
    fun patchUser(
        userId: String,
        patchOp: PatchOp,
    ): User? =
        transaction {
            try {
                val uuid = UUID.fromString(userId)
                val existingUserRow = Users.select { Users.id eq uuid }.singleOrNull() ?: return@transaction null

                // 기존 사용자 데이터를 객체로 변환
                var patchedUser = resultRowToUser(existingUserRow)

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
                val created = existingUserRow[Users.created]
                val meta =
                    Meta(
                        resourceType = "User",
                        created = created,
                        lastModified = now,
                        location = "/Users/$userId",
                    )

                // 복잡한 필드를 JSON으로 변환
                val nameJson = patchedUser.name?.let { objectMapper.writeValueAsString(it) }
                val emailsJson = patchedUser.emails?.let { objectMapper.writeValueAsString(it) }
                val metaJson = objectMapper.writeValueAsString(meta)

                // 데이터베이스 업데이트
                Users.update({ Users.id eq uuid }) { row ->
                    row[externalId] = patchedUser.externalId
                    row[userName] = patchedUser.userName
                    row[displayName] = patchedUser.displayName
                    row[active] = patchedUser.active
                    row[lastModified] = now
                    row[Users.nameJson] = nameJson
                    row[Users.emailsJson] = emailsJson
                    row[Users.metaJson] = metaJson
                    row[nickName] = patchedUser.nickName
                    row[profileUrl] = patchedUser.profileUrl
                    row[title] = patchedUser.title
                    row[userType] = patchedUser.userType
                    row[preferredLanguage] = patchedUser.preferredLanguage
                    row[locale] = patchedUser.locale
                    row[timezone] = patchedUser.timezone
                }

                // 업데이트된 사용자 반환
                patchedUser.copy(meta = meta)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $userId")
                null
            }
        }

    /**
     * 사용자 삭제
     */
    fun deleteUser(userId: String): Boolean =
        transaction {
            try {
                val uuid = UUID.fromString(userId)
                val deletedRowCount = Users.deleteWhere { Users.id eq uuid }
                deletedRowCount > 0
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid UUID format: $userId")
                false
            }
        }
}
