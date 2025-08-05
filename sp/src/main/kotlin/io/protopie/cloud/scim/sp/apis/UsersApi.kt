package io.protopie.cloud.scim.sp.apis

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.protopie.cloud.scim.sp.models.Error
import io.protopie.cloud.scim.sp.models.PatchOp
import io.protopie.cloud.scim.sp.models.User
import io.protopie.cloud.scim.sp.service.UserService
import io.protopie.cloud.scim.sp.utils.respondScim
import io.protopie.cloud.scim.sp.utils.respondScimNoContent

/**
 * SCIM Users API 라우팅 설정
 */
class UsersApi {
    private val userService = UserService()

    /**
     * UserService 인스턴스를 반환
     */
    fun getUserService(): UserService = userService

    /**
     * Users API 라우팅 설정
     */
    fun Application.configureUsersApi() {
        routing {
            authenticate("scimAuth") {
                route("/scim/v2") {
                    // 사용자 목록 조회
                    get("/Users") {
                        val startIndex = call.request.queryParameters["startIndex"]?.toIntOrNull() ?: 1
                        val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 100
                        val filter = call.request.queryParameters["filter"]

                        // 필터가 있는 경우 필터링된 결과 반환 (향후 구현)
                        val userList = userService.getUsers(startIndex, count)
                        call.respondScim(HttpStatusCode.OK, userList)
                    }
                    // 사용자 생성
                    post("/Users") {
                        try {
                            val user = call.receive<User>()
                            val createdUser = userService.createUser(user)
                            call.respondScim(HttpStatusCode.Created, createdUser)
                        } catch (e: IllegalStateException) {
                            val error =
                                Error(
                                    detail = e.message ?: "User already exists",
                                    status = "409",
                                )
                            call.respondScim(HttpStatusCode.Conflict, error)
                        }
                    }

                    // 특정 사용자 조회
                    get("/Users/{externalId}") {
                        val externalId =
                            call.parameters["externalId"]
                                ?: throw BadRequestException("Missing externalId parameter")

                        val user = userService.getUserById(externalId)
                        if (user != null) {
                            call.respondScim(HttpStatusCode.OK, user)
                        } else {
                            val error =
                                Error(
                                    detail = "User with ID $externalId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 사용자 정보 전체 교체
                    put("/Users/{externalId}") {
                        val externalId =
                            call.parameters["externalId"]
                                ?: throw BadRequestException("Missing externalId parameter")
                        val user = call.receive<User>()
                        val updatedUser = userService.updateUser(externalId, user)

                        if (updatedUser != null) {
                            call.respondScim(HttpStatusCode.OK, updatedUser)
                        } else {
                            val error =
                                Error(
                                    detail = "User with ID $externalId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 사용자 정보 부분 수정
                    patch("/Users/{externalId}") {
                        val externalId =
                            call.parameters["externalId"]
                                ?: throw BadRequestException("Missing externalId parameter")
                        val patchOp = call.receive<PatchOp>()
                        val patchedUser = userService.patchUser(externalId, patchOp)

                        if (patchedUser != null) {
                            call.respondScim(HttpStatusCode.OK, patchedUser)
                        } else {
                            val error =
                                Error(
                                    detail = "User with ID $externalId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 사용자 삭제
                    delete("/Users/{externalId}") {
                        val externalId =
                            call.parameters["externalId"]
                                ?: throw BadRequestException("Missing externalId parameter")

                        val deleted = userService.deleteUser(externalId)
                        if (deleted) {
                            call.respondScimNoContent()
                        } else {
                            val error =
                                Error(
                                    detail = "User with ID $externalId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }
                }
            }
        }
    }
}
