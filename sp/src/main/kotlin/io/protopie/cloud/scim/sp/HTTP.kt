package io.protopie.cloud.scim.sp

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.slf4j.event.Level

/**
 * HTTP 관련 설정
 */
fun Application.configureHTTP() {
    // 접근 로그 설정
    install(CallLogging) {
        level = Level.INFO

        // 필터 - 모든 요청에 대해 로깅
        filter { call -> true }

        // MDC에 요청 정보 추가
        mdc("request-id") { call ->
            call.request.header("X-Request-ID") ?: call.request.header("X-Topo-Request-ID") ?: call.request.uri
        }

        // 로그 형식 지정
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val contentType = call.request.contentType().toString()

            "$status: $httpMethod $path | UA: $userAgent | ContentType: $contentType"
        }
    }

    routing {
        swaggerUI(path = "openapi")
    }
}
