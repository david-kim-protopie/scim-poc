package io.protopie.cloud.scim.sp.service

import io.protopie.cloud.scim.sp.models.*

/**
 * SCIM Discovery 관련 서비스
 */
class DiscoveryService {
    /**
     * 서비스 제공자 설정 정보 조회
     */
    fun getServiceProviderConfig(): ServiceProviderConfig = ServiceProviderConfig()

    /**
     * 지원하는 스키마 목록 조회
     */
    fun getSchemas(): SchemaListResponse {
        val schemas =
            listOf(
                createUserSchema(),
                createGroupSchema(),
            )

        return SchemaListResponse(
            totalResults = schemas.size,
            startIndex = 1,
            itemsPerPage = schemas.size,
            resources = schemas,
        )
    }

    /**
     * 특정 URN의 스키마 조회
     */
    fun getSchemaByUrn(schemaUrn: String): SchemaDefinition? =
        when (schemaUrn) {
            "urn:ietf:params:scim:schemas:core:2.0:User" -> createUserSchema()
            "urn:ietf:params:scim:schemas:core:2.0:Group" -> createGroupSchema()
            else -> null
        }

    /**
     * 지원하는 리소스 타입 목록 조회
     */
    fun getResourceTypes(): ResourceTypeListResponse {
        val resourceTypes =
            listOf(
                ResourceType(
                    id = "User",
                    name = "User",
                    endpoint = "/Users",
                    description = "사용자 리소스",
                    schema = "urn:ietf:params:scim:schemas:core:2.0:User",
                    meta =
                        Meta(
                            location = "/ResourceTypes/User",
                            resourceType = "Schema",
                        ),
                ),
                ResourceType(
                    id = "Group",
                    name = "Group",
                    endpoint = "/Groups",
                    description = "그룹 리소스",
                    schema = "urn:ietf:params:scim:schemas:core:2.0:Group",
                    meta =
                        Meta(
                            location = "/ResourceTypes/Group",
                            resourceType = "Schema",
                        ),
                ),
            )

        return ResourceTypeListResponse(
            totalResults = resourceTypes.size,
            startIndex = 1,
            itemsPerPage = resourceTypes.size,
            resources = resourceTypes,
        )
    }

    /**
     * User 스키마 생성
     */
    private fun createUserSchema(): SchemaDefinition =
        SchemaDefinition(
            id = "urn:ietf:params:scim:schemas:core:2.0:User",
            name = "User",
            description = "사용자 리소스 스키마 (RFC 7643 Section 4.1)",
            attributes =
                listOf(
                    SchemaAttribute(
                        name = "id",
                        type = "string",
                        multiValued = false,
                        description = "서비스 제공자(SP)에 의해 할당된 고유 식별자",
                        required = false,
                        mutability = "readOnly",
                        returned = "always",
                        uniqueness = "server",
                    ),
                    SchemaAttribute(
                        name = "externalId",
                        type = "string",
                        multiValued = false,
                        description = "IdP에서 사용하는 사용자의 고유 식별자",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                    ),
                    SchemaAttribute(
                        name = "userName",
                        type = "string",
                        multiValued = false,
                        description = "사용자 식별을 위한 고유한 값 (예: 이메일 주소)",
                        required = true,
                        mutability = "readWrite",
                        returned = "always",
                        uniqueness = "server",
                    ),
                    SchemaAttribute(
                        name = "name",
                        type = "complex",
                        multiValued = false,
                        description = "사용자의 실명 정보",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "formatted",
                                    type = "string",
                                    multiValued = false,
                                    description = "전체 이름 (예: Ms. Barbara J Jensen, III)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "familyName",
                                    type = "string",
                                    multiValued = false,
                                    description = "성 (예: Jensen)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "givenName",
                                    type = "string",
                                    multiValued = false,
                                    description = "이름 (예: Barbara)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "middleName",
                                    type = "string",
                                    multiValued = false,
                                    description = "중간 이름",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "honorificPrefix",
                                    type = "string",
                                    multiValued = false,
                                    description = "접두사 (예: Ms.)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "honorificSuffix",
                                    type = "string",
                                    multiValued = false,
                                    description = "접미사 (예: III)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                            ),
                    ),
                    SchemaAttribute(
                        name = "displayName",
                        type = "string",
                        multiValued = false,
                        description = "UI에 표시하기 적합한 이름",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                    ),
                    SchemaAttribute(
                        name = "active",
                        type = "boolean",
                        multiValued = false,
                        description = "계정 활성 상태",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                    ),
                    SchemaAttribute(
                        name = "emails",
                        type = "complex",
                        multiValued = true,
                        description = "사용자 이메일 주소",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "value",
                                    type = "string",
                                    multiValued = false,
                                    description = "이메일 주소",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "display",
                                    type = "string",
                                    multiValued = false,
                                    description = "표시 이름",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "primary",
                                    type = "boolean",
                                    multiValued = false,
                                    description = "대표 이메일 여부",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "type",
                                    type = "string",
                                    multiValued = false,
                                    description = "이메일 유형(work, home 등)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                            ),
                    ),
                ),
        )

    /**
     * Group 스키마 생성
     */
    private fun createGroupSchema(): SchemaDefinition =
        SchemaDefinition(
            id = "urn:ietf:params:scim:schemas:core:2.0:Group",
            name = "Group",
            description = "그룹 리소스 스키마 (RFC 7643 Section 4.2)",
            attributes =
                listOf(
                    SchemaAttribute(
                        name = "id",
                        type = "string",
                        multiValued = false,
                        description = "그룹 고유 식별자",
                        required = false,
                        mutability = "readOnly",
                        returned = "always",
                        uniqueness = "server",
                    ),
                    SchemaAttribute(
                        name = "externalId",
                        type = "string",
                        multiValued = false,
                        description = "IdP에서 사용하는 그룹의 고유 식별자",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                    ),
                    SchemaAttribute(
                        name = "displayName",
                        type = "string",
                        multiValued = false,
                        description = "그룹의 표시 이름",
                        required = true,
                        mutability = "readWrite",
                        returned = "always",
                        uniqueness = "server",
                    ),
                    SchemaAttribute(
                        name = "members",
                        type = "complex",
                        multiValued = true,
                        description = "그룹 멤버 목록",
                        required = false,
                        mutability = "readWrite",
                        returned = "default",
                        uniqueness = "none",
                        subAttributes =
                            listOf(
                                SchemaAttribute(
                                    name = "value",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버의 고유 ID (User의 id)",
                                    required = true,
                                    mutability = "immutable",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "ref",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버 리소스의 URI",
                                    required = false,
                                    mutability = "readOnly",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "display",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버 표시 이름",
                                    required = false,
                                    mutability = "readOnly",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                                SchemaAttribute(
                                    name = "type",
                                    type = "string",
                                    multiValued = false,
                                    description = "멤버 타입 (User 또는 Group)",
                                    required = false,
                                    mutability = "readWrite",
                                    returned = "default",
                                    uniqueness = "none",
                                ),
                            ),
                    ),
                ),
        )
}
