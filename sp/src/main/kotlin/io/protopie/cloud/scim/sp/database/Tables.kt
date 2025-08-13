package io.protopie.cloud.scim.sp.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Users 테이블 정의
 */
object Users : UUIDTable() {
    val externalId = varchar("external_id", 255).nullable()
    val userName = varchar("user_name", 255).uniqueIndex()
    val displayName = varchar("display_name", 255).nullable()
    val active = bool("active").default(true)
    val created = datetime("created").default(LocalDateTime.now())
    val lastModified = datetime("last_modified").default(LocalDateTime.now())

    // 복잡한 필드는 JSON으로 저장
    val nameJson = text("name_json").nullable()
    val emailsJson = text("emails_json").nullable()
    val phoneNumbersJson = text("phone_numbers_json").nullable()
    val addressesJson = text("addresses_json").nullable()
    val groupsJson = text("groups_json").nullable()
    val metaJson = text("meta_json").nullable()

    // 추가 필드
    val nickName = varchar("nick_name", 255).nullable()
    val profileUrl = varchar("profile_url", 1024).nullable()
    val title = varchar("title", 255).nullable()
    val userType = varchar("user_type", 255).nullable()
    val preferredLanguage = varchar("preferred_language", 50).nullable()
    val locale = varchar("locale", 50).nullable()
    val timezone = varchar("timezone", 50).nullable()
}
