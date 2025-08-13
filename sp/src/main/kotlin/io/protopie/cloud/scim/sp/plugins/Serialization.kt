package io.protopie.cloud.scim.sp.plugins

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

/**
 * 직렬화 설정
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            // 역직렬화 실패 방지 설정
            configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            // null 직렬화 설정
            configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_NULL_MAP_VALUES, false)
            // 날짜 처리 설정
            findAndRegisterModules()
        }
    }
}
