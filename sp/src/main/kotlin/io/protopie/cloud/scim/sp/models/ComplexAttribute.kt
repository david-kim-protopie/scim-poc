package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 복합 속성 모델
 * 이메일, 전화번호 등 값과 타입을 함께 저장하는 속성에 사용
 */
data class ComplexAttribute(
    @JsonProperty("value")
    val value: String,

    @JsonProperty("display")
    val display: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("primary")
    val primary: Boolean = false
)
