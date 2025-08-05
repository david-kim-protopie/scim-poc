package io.protopie.cloud.scim.sp

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

/**
 * 애플리케이션 모듈 설정
 */
fun Application.module() {
    // CORS 설정
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    // 에러 핸들링
    install(StatusPages) {
//        exception<Throwable> { call, cause ->
//            val error =
//                Error(
//                    schemas = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
//                    detail = cause.message ?: "Internal Server Error",
//                    status = "500",
//                )
//            call.respondScim(HttpStatusCode.InternalServerError, error)
//        }
    }

    // 인증 설정
    install(Authentication) {
        bearer("scimAuth") {
            authenticate { tokenCredential ->
                // 실제 환경에서는 토큰 검증 로직이 필요
                // 개발/테스트 목적으로 항상 인증 성공 처리
                UserIdPrincipal("idp-client")
            }
        }
    }

    // HTTP 설정
    configureHTTP()

    // 라우팅 설정
    configureRouting()
}
