package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 서비스 제공자 설정
 * RFC 7643 Section 5 기반
 */
data class ServiceProviderConfig(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"),
    @JsonProperty("documentationUri")
    val documentationUri: String = "https://tools.ietf.org/html/rfc7644",
    @JsonProperty("patch")
    val patch: SupportedFeature = SupportedFeature(true),
    @JsonProperty("bulk")
    val bulk: BulkConfig = BulkConfig(false),
    @JsonProperty("filter")
    val filter: FilterConfig = FilterConfig(true, 200),
    @JsonProperty("changePassword")
    val changePassword: SupportedFeature = SupportedFeature(true),
    @JsonProperty("sort")
    val sort: SupportedFeature = SupportedFeature(true),
    @JsonProperty("etag")
    val etag: SupportedFeature = SupportedFeature(false),
    @JsonProperty("authenticationSchemes")
    val authenticationSchemes: List<AuthScheme> =
        listOf(
            AuthScheme(
                name = "Bearer Token",
                description = "Bearer 토큰 인증",
                type = "bearer",
                primary = true,
            ),
        ),
    @JsonProperty("meta")
    val meta: Meta =
        Meta(
            resourceType = "ServiceProviderConfig",
            location = "/ServiceProviderConfig",
        ),
)

/**
 * 기능 지원 여부
 */
data class SupportedFeature(
    @JsonProperty("supported")
    val supported: Boolean,
)

/**
 * 대량 작업 설정
 */
data class BulkConfig(
    @JsonProperty("supported")
    val supported: Boolean,
    @JsonProperty("maxOperations")
    val maxOperations: Int = 1000,
    @JsonProperty("maxPayloadSize")
    val maxPayloadSize: Int = 1048576, // 1MB
)

/**
 * 인증 스키마
 */
data class AuthScheme(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("primary")
    val primary: Boolean = false,
)

/**
 * 필터링 설정
 */
data class FilterConfig(
    @JsonProperty("supported")
    val supported: Boolean,
    @JsonProperty("maxResults")
    val maxResults: Int,
)
