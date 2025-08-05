package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 에러 응답 모델
 */
data class Error(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
    @JsonProperty("detail")
    val detail: String,
    @JsonProperty("status")
    val status: String,
)
