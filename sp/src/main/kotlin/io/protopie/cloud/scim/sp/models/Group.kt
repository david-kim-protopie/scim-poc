package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM Group 리소스
 * RFC 7643 Section 4.2 기반
 */
data class Group(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:schemas:core:2.0:Group"),
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("externalId")
    val externalId: String? = null,
    @JsonProperty("displayName")
    val displayName: String,
    @JsonProperty("members")
    val members: List<Member>? = null,
    @JsonProperty("meta")
    val meta: Meta? = null,
)
