package io.protopie.cloud.scim.sp.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

/**
 * 보안 설정
 */
fun Application.configureSecurity() {
    // 기본 인증 설정
    install(Authentication) {
        basic("scimAuth") {
            validate { credentials ->
                // 실제 환경에서는 실제 사용자 검증 로직을 구현해야 함
                // 현재는 모든 사용자를 허용
                UserIdPrincipal(credentials.name)
            }
        }
    }
}
