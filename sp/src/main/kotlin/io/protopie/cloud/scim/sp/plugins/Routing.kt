package io.protopie.cloud.scim.sp.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.protopie.cloud.scim.sp.apis.GroupsApi
import io.protopie.cloud.scim.sp.apis.UsersApi

/**
 * 라우팅 설정
 */
fun Application.configureRouting() {
    val usersApi = UsersApi()
    val groupsApi = GroupsApi(usersApi.getUserService())

    usersApi.apply { configureUsersApi() }
    groupsApi.apply { configureGroupsApi() }

    routing {
        get("/") {
            call.respondText("SCIM API Server")
        }
    }
}
