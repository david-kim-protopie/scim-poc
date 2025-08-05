package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 리소스 타입 정의
 * RFC 7643 Section 6 기반
 */
data class ResourceType(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
    @JsonProperty("id")
    val id: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("endpoint")
    val endpoint: String,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("schema")
    val schema: String,
    @JsonProperty("schemaExtensions")
    val schemaExtensions: List<SchemaExtension>? = null,
    @JsonProperty("meta")
    val meta: Meta? = null,
)

/**
 * 스키마 확장 정의
 */
data class SchemaExtension(
    @JsonProperty("description")
    val description: String,
    @JsonProperty("schema")
    val schema: String,
    @JsonProperty("meta")
    val meta: ResourceTypeMeta,
)

/**
 * 리소스 타입 메타 정보
 */
data class ResourceTypeMeta(
    @JsonProperty("location")
    val location: String,
    @JsonProperty("resourceType")
    val resourceType: String = "ResourceType",
)

/**
 * 리소스 타입 목록 응답
 */
typealias ResourceTypeListResponse = ListResponse<ResourceType>
