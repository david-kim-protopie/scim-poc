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
 * SCIM PATCH 작업 항목
 * RFC7644에 따른 PATCH 작업 정의
 * 다음과 같은 두 가지 방식을 모두 지원합니다:
 *
 * 1. 'path'가 없는 경우: 'value'에 포함된 속성 기반으로 동작합니다.
 * 예시:
 * {
 *   "op": "replace",
 *   "value": {
 *     "name": { "givenName": "David" }
 *   }
 * }
 *
 * 2. 'path'가 있는 경우: 'path' 값에 따라 특정 속성이나 필터링된 요소를 대상으로 작업합니다.
 * 예시:
 * {
 *   "op": "remove",
 *   "path": "members[value eq \"36990822-eda3-4f4a-882a-abef29ae8905\"]"
 * }
 */
data class Operation(
    @JsonProperty("op")
    val op: String, // "add", "replace", "remove"
    @JsonProperty("path")
    val path: String? = null,
    @JsonProperty("value")
    val value: JsonNode? = null,
) {
    override fun toString(): String = "Operation(op='$op', path=$path, value=${value?.toString() ?: "null"})"
}
