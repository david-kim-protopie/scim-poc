package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * SCIM 패치 작업 모델
 */
data class PatchOp(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp"),
    @JsonProperty("Operations")
    val operations: List<Operation>,
)

/**
 * 패치 작업 항목
 */
data class Operation(
    @JsonProperty("op")
    val op: String, // "add", "replace", "remove"
    @JsonProperty("path")
    val path: String? = null,
    @JsonProperty("value")
    val value: JsonNode? = null,
)
