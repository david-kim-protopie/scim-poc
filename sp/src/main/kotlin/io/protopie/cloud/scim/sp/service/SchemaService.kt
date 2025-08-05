package io.protopie.cloud.scim.sp.service

import io.protopie.cloud.scim.sp.models.Meta
import io.protopie.cloud.scim.sp.models.SchemaAttribute
import io.protopie.cloud.scim.sp.models.SchemaDefinition

/**
 * SCIM 스키마 서비스
 */
class SchemaService {
    // 스키마 정의 저장소
    private val schemas: MutableMap<String, SchemaDefinition> = mutableMapOf()

    init {
        // User 스키마 초기화
        val userSchema = createUserSchema()
        schemas[userSchema.id] = userSchema

        // Group 스키마 초기화
        val groupSchema = createGroupSchema()
        schemas[groupSchema.id] = groupSchema
    }

    /**
     * 모든 스키마 목록 조회
     */
    fun getSchemas(): List<SchemaDefinition> = schemas.values.toList()

    /**
     * 특정 스키마 조회
     */
    fun getSchemaById(schemaId: String): SchemaDefinition? = schemas[schemaId]

    /**
     * User 스키마 생성
     */
    private fun createUserSchema(): SchemaDefinition =
        SchemaDefinition(
            id = "urn:ietf:params:scim:schemas:core:2.0:User",
            name = "User",
            description = "User Account",
            attributes =
                listOf(
                    SchemaAttribute(
                        name = "id",
                        type = "string",
                        multiValued = false,
                        description = "서비스 제공자에 의해 할당된 고유 식별자",
                        required = false,
                        mutability = "readOnly",
                    ),
                    SchemaAttribute(
                        name = "externalId",
                        type = "string",
                        multiValued = false,
                        description = "IdP에서 사용하는 사용자의 고유 식별자",
                        required = false,
                    ),
                    SchemaAttribute(
                        name = "userName",
                        type = "string",
                        multiValued = false,
                        description = "사용자 식별을 위한 고유한 값 (예: 이메일 주소)",
                        required = true,
                        caseExact = false,
                    ),
                    SchemaAttribute(
                        name = "name",
                        type = "complex",
                        multiValued = false,
                        description = "사용자의 이름 정보",
                        required = false,
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "formatted",
                                    type = "string",
                                    multiValued = false,
                                    description = "전체 이름 (예: Ms. Barbara J Jensen, III)",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "familyName",
                                    type = "string",
                                    multiValued = false,
                                    description = "성 (예: Jensen)",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "givenName",
                                    type = "string",
                                    multiValued = false,
                                    description = "이름 (예: Barbara)",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "middleName",
                                    type = "string",
                                    multiValued = false,
                                    description = "중간 이름",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "honorificPrefix",
                                    type = "string",
                                    multiValued = false,
                                    description = "접두사 (예: Ms.)",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "honorificSuffix",
                                    type = "string",
                                    multiValued = false,
                                    description = "접미사 (예: III)",
                                    required = false,
                                ),
                            ),
                    ),
                    SchemaAttribute(
                        name = "displayName",
                        type = "string",
                        multiValued = false,
                        description = "UI에 표시하기 적합한 이름",
                        required = false,
                    ),
                    SchemaAttribute(
                        name = "emails",
                        type = "complex",
                        multiValued = true,
                        description = "사용자의 이메일 주소",
                        required = false,
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "value",
                                    type = "string",
                                    multiValued = false,
                                    description = "이메일 주소 값",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "type",
                                    type = "string",
                                    multiValued = false,
                                    description = "이메일 유형 (work, home, other 등)",
                                    required = false,
                                    canonicalValues = listOf("work", "home", "other"),
                                ),
                                SchemaAttribute(
                                    name = "primary",
                                    type = "boolean",
                                    multiValued = false,
                                    description = "기본 이메일 주소 여부",
                                    required = false,
                                ),
                            ),
                    ),
                    SchemaAttribute(
                        name = "active",
                        type = "boolean",
                        multiValued = false,
                        description = "계정 활성 상태",
                        required = false,
                    ),
                    SchemaAttribute(
                        name = "password",
                        type = "string",
                        multiValued = false,
                        description = "사용자 계정 비밀번호",
                        required = false,
                        mutability = "writeOnly",
                        returned = "never",
                    ),
                    // 기타 필요한 속성들은 실제 구현에 맞게 추가
                ),
            meta =
                Meta(
                    resourceType = "Schema",
                    location = "/Schemas/urn:ietf:params:scim:schemas:core:2.0:User",
                ),
        )

    /**
     * Group 스키마 생성
     */
    private fun createGroupSchema(): SchemaDefinition =
        SchemaDefinition(
            id = "urn:ietf:params:scim:schemas:core:2.0:Group",
            name = "Group",
            description = "Group",
            attributes =
                listOf(
                    SchemaAttribute(
                        name = "id",
                        type = "string",
                        multiValued = false,
                        description = "서비스 제공자에 의해 할당된 고유 식별자",
                        required = false,
                        mutability = "readOnly",
                    ),
                    SchemaAttribute(
                        name = "externalId",
                        type = "string",
                        multiValued = false,
                        description = "IdP에서 사용하는 그룹의 고유 식별자",
                        required = false,
                    ),
                    SchemaAttribute(
                        name = "displayName",
                        type = "string",
                        multiValued = false,
                        description = "그룹의 표시 이름",
                        required = true,
                    ),
                    SchemaAttribute(
                        name = "members",
                        type = "complex",
                        multiValued = true,
                        description = "그룹의 멤버 목록",
                        required = false,
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "value",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버의 고유 ID (User의 id)",
                                    required = false,
                                ),
                                SchemaAttribute(
                                    name = "ref",
                                    type = "reference",
                                    multiValued = false,
                                    description = "멤버 리소스의 URI",
                                    required = false,
                                    mutability = "readOnly",
                                    referenceTypes = listOf("User", "Group"),
                                ),
                                SchemaAttribute(
                                    name = "display",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버의 표시 이름",
                                    required = false,
                                    mutability = "readOnly",
                                ),
                                SchemaAttribute(
                                    name = "type",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버의 유형",
                                    required = false,
                                    canonicalValues = listOf("User", "Group"),
                                ),
                            ),
                    ),
                    // 기타 필요한 속성들은 실제 구현에 맞게 추가
                ),
            meta =
                Meta(
                    resourceType = "Schema",
                    location = "/Schemas/urn:ietf:params:scim:schemas:core:2.0:Group",
                ),
        )
}
