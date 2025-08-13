package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 그룹 멤버 모델
 */
data class Member(
    @JsonProperty("value")
    val value: String,
    @JsonProperty("ref")
    val ref: String? = null,
    @JsonProperty("display")
    val display: String? = null,
    @JsonProperty("type")
    val type: String? = "User",
)
