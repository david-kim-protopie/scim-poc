package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM User 리소스
 * RFC 7643 Section 4.1 기반
 */
data class User(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:schemas:core:2.0:User"),
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("externalId")
    val externalId: String? = null,
    @JsonProperty("userName")
    val userName: String,
    @JsonProperty("name")
    val name: Name? = null,
    @JsonProperty("displayName")
    val displayName: String? = null,
    @JsonProperty("nickName")
    val nickName: String? = null,
    @JsonProperty("profileUrl")
    val profileUrl: String? = null,
    @JsonProperty("title")
    val title: String? = null,
    @JsonProperty("userType")
    val userType: String? = null,
    @JsonProperty("preferredLanguage")
    val preferredLanguage: String? = null,
    @JsonProperty("locale")
    val locale: String? = null,
    @JsonProperty("timezone")
    val timezone: String? = null,
    @JsonProperty("active")
    val active: Boolean = true,
    @JsonProperty("password")
    val password: String? = null,
    @JsonProperty("emails")
    val emails: List<ComplexAttribute>? = null,
    @JsonProperty("phoneNumbers")
    val phoneNumbers: List<ComplexAttribute>? = null,
    @JsonProperty("ims")
    val ims: List<ComplexAttribute>? = null,
    @JsonProperty("photos")
    val photos: List<ComplexAttribute>? = null,
    @JsonProperty("addresses")
    val addresses: List<Address>? = null,
    @JsonProperty("groups")
    val groups: List<Member>? = null,
    @JsonProperty("entitlements")
    val entitlements: List<ComplexAttribute>? = null,
    @JsonProperty("roles")
    val roles: List<ComplexAttribute>? = null,
    @JsonProperty("x509Certificates")
    val x509Certificates: List<ComplexAttribute>? = null,
    @JsonProperty("meta")
    val meta: Meta? = null,
)

/**
 * 사용자 이름 정보
 */
data class Name(
    @JsonProperty("formatted")
    val formatted: String? = null,
    @JsonProperty("familyName")
    val familyName: String? = null,
    @JsonProperty("givenName")
    val givenName: String? = null,
    @JsonProperty("middleName")
    val middleName: String? = null,
    @JsonProperty("honorificPrefix")
    val honorificPrefix: String? = null,
    @JsonProperty("honorificSuffix")
    val honorificSuffix: String? = null,
)
