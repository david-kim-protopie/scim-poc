package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 주소 모델
 */
data class Address(
    @JsonProperty("formatted")
    val formatted: String? = null,

    @JsonProperty("streetAddress")
    val streetAddress: String? = null,

    @JsonProperty("locality")
    val locality: String? = null,

    @JsonProperty("region")
    val region: String? = null,

    @JsonProperty("postalCode")
    val postalCode: String? = null,

    @JsonProperty("country")
    val country: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("primary")
    val primary: Boolean = false
)
