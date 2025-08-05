package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * 리소스 메타데이터 정보
 */
data class Meta(
    @JsonProperty("resourceType")
    val resourceType: String,
    @JsonProperty("created")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val created: LocalDateTime? = null,
    @JsonProperty("lastModified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val lastModified: LocalDateTime? = null,
    @JsonProperty("location")
    val location: String? = null,
    @JsonProperty("version")
    val version: String? = null,
)
