package io.protopie.cloud.scim.sp

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.protopie.cloud.scim.sp.apis.DiscoveryApi
import io.protopie.cloud.scim.sp.apis.GroupsApi
import io.protopie.cloud.scim.sp.apis.UsersApi

/**
 * 라우팅 설정
 */
fun Application.configureRouting() {
    // 전역 Jackson 설정
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
        // SCIM 미디어 타입에 대한 설정
        jackson(ContentType.parse("application/scim+json")) {
            registerModule(JavaTimeModule())
        }
    }

    // 기본 라우팅 설정
    routing {
        get("/") {
            call.respondText("SCIM Service Provider API is running")
        }
    }

    // UsersApi 라우팅 설정 적용
    val usersApi = UsersApi()
    with(usersApi) {
        configureUsersApi()
    }

    // GroupsApi 라우팅 설정 적용
    val groupsApi = GroupsApi(usersApi.getUserService())
    with(groupsApi) {
        configureGroupsApi()
    }

    // DiscoveryApi 라우팅 설정 적용
    val discoveryApi = DiscoveryApi()
    with(discoveryApi) {
        configureDiscoveryApi()
    }
}
