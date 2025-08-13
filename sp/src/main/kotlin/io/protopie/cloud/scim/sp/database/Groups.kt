package io.protopie.cloud.scim.sp.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Groups 테이블 정의
 */
object Groups : UUIDTable() {
    val externalId = varchar("external_id", 255).nullable()
    val displayName = varchar("display_name", 255)
    val created = datetime("created").default(LocalDateTime.now())
    val lastModified = datetime("last_modified").default(LocalDateTime.now())

    // 복잡한 필드는 JSON으로 저장
    val membersJson = text("members_json").nullable()
    val metaJson = text("meta_json").nullable()
}
