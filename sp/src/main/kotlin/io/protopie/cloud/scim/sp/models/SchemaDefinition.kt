package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 스키마 정의
 * RFC 7643 Section 7 기반
 */
data class SchemaDefinition(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("attributes")
    val attributes: List<SchemaAttribute>,
    @JsonProperty("meta")
    val meta: Meta? = null,
)

/**
 * SCIM 스키마 속성 정의
 */
data class SchemaAttribute(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("multiValued")
    val multiValued: Boolean,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("required")
    val required: Boolean,
    @JsonProperty("caseExact")
    val caseExact: Boolean = false,
    @JsonProperty("mutability")
    val mutability: String = "readWrite",
    @JsonProperty("returned")
    val returned: String = "default",
    @JsonProperty("uniqueness")
    val uniqueness: String = "none",
    @JsonProperty("subAttributes")
    val subAttributes: List<SchemaAttribute> = listOf(),
    @JsonProperty("canonicalValues")
    val canonicalValues: List<String> = listOf(),
    @JsonProperty("referenceTypes")
    val referenceTypes: List<String> = listOf(),
)

/**
 * 스키마 목록 응답
 */
typealias SchemaListResponse = ListResponse<SchemaDefinition>
