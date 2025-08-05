package io.protopie.cloud.scim.sp.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * SCIM 미디어 타입으로 응답을 반환합니다.
 */
suspend inline fun <reified T : Any> ApplicationCall.respondScim(
    status: HttpStatusCode = HttpStatusCode.OK,
    noinline builder: suspend () -> T,
) {
    // SCIM 미디어 타입 헤더 설정
    response.header(HttpHeaders.ContentType, "application/scim+json")

    // 응답 전송
    respond(status, builder())
}

/**
 * SCIM 미디어 타입으로 응답을 반환합니다.
 */
suspend inline fun <reified T : Any> ApplicationCall.respondScim(
    status: HttpStatusCode = HttpStatusCode.OK,
    message: T,
) {
    // SCIM 미디어 타입 헤더 설정
    this.response.header(HttpHeaders.ContentType, "application/scim+json")

    // 응답 전송
    respond(status, message)
}

/**
 * SCIM 미디어 타입으로 응답을 반환합니다 (NoContent 응답용).
 */
suspend fun ApplicationCall.respondScimNoContent() {
    // SCIM 미디어 타입 헤더 설정
    response.header(HttpHeaders.ContentType, "application/scim+json")

    // 응답 전송
    respond(HttpStatusCode.NoContent)
}
