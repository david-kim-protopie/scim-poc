package io.protopie.cloud.scim.sp.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SCIM 목록 응답 모델
 * @param T 리소스 타입
 */
data class ListResponse<T>(
    @JsonProperty("schemas")
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
    @JsonProperty("totalResults")
    val totalResults: Int,
    @JsonProperty("startIndex")
    val startIndex: Int = 1,
    @JsonProperty("itemsPerPage")
    val itemsPerPage: Int = 0,
    @JsonProperty("Resources")
    val resources: List<T>,
)

/**
 * 사용자 목록 응답 타입 별칭
 */
typealias UserListResponse = ListResponse<User>

/**
 * 그룹 목록 응답 타입 별칭
 */
typealias GroupListResponse = ListResponse<Group>
