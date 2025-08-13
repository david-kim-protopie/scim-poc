package io.protopie.cloud.scim.sp.apis

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.protopie.cloud.scim.sp.models.Error
import io.protopie.cloud.scim.sp.models.Group
import io.protopie.cloud.scim.sp.models.PatchOp
import io.protopie.cloud.scim.sp.service.GroupService
import io.protopie.cloud.scim.sp.service.UserService
import io.protopie.cloud.scim.sp.utils.respondScim
import io.protopie.cloud.scim.sp.utils.respondScimNoContent

/**
 * SCIM Groups API 라우팅 설정
 */
class GroupsApi(
    userService: UserService,
) {
    private val groupService = GroupService(userService)

    // 테스트를 위한 접근자 메서드
    fun getGroupService(): GroupService = groupService

    /**
     * Groups API 라우팅 설정
     */
    fun Application.configureGroupsApi() {
        routing {
            authenticate("scimAuth") {
                route("/scim/v2") {
                    // 그룹 목록 조회
                    get("/Groups") {
                        val startIndex = call.request.queryParameters["startIndex"]?.toIntOrNull() ?: 1
                        val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 100

                        val groupList = groupService.getGroups(startIndex, count)
                        call.respondScim(HttpStatusCode.OK, groupList)
                    }
                    // 그룹 생성
                    post("/Groups") {
                        try {
                            val group = call.receive<Group>()
                            val createdGroup = groupService.createGroup(group)
                            call.respondScim(HttpStatusCode.Created, createdGroup)
                        } catch (e: IllegalStateException) {
                            val error =
                                Error(
                                    detail = e.message ?: "Group already exists",
                                    status = "409",
                                )
                            call.respondScim(HttpStatusCode.Conflict, error)
                        }
                    }

                    // 특정 그룹 조회
                    get("/Groups/{groupId}") {
                        val groupId =
                            call.parameters["groupId"]
                                ?: throw BadRequestException("Missing groupId parameter")

                        val group = groupService.getGroupById(groupId)
                        if (group != null) {
                            call.respondScim(HttpStatusCode.OK, group)
                        } else {
                            val error =
                                Error(
                                    detail = "Group with ID $groupId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 그룹 정보 전체 교체
                    put("/Groups/{groupId}") {
                        val groupId =
                            call.parameters["groupId"]
                                ?: throw BadRequestException("Missing groupId parameter")

                        val group = call.receive<Group>()
                        val updatedGroup = groupService.updateGroup(groupId, group)

                        if (updatedGroup != null) {
                            call.respondScim(HttpStatusCode.OK, updatedGroup)
                        } else {
                            val error =
                                Error(
                                    detail = "Group with ID $groupId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 그룹 정보 부분 수정
                    patch("/Groups/{groupId}") {
                        val groupId =
                            call.parameters["groupId"]
                                ?: throw BadRequestException("Missing groupId parameter")

                        val patchOp = call.receive<PatchOp>()
                        val patchedGroup = groupService.patchGroup(groupId, patchOp)

                        if (patchedGroup != null) {
                            call.respondScim(HttpStatusCode.OK, patchedGroup)
                        } else {
                            val error =
                                Error(
                                    detail = "Group with ID $groupId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 그룹 삭제
                    delete("/Groups/{groupId}") {
                        val groupId =
                            call.parameters["groupId"]
                                ?: throw BadRequestException("Missing groupId parameter")

                        val deleted = groupService.deleteGroup(groupId)
                        if (deleted) {
                            call.respondScimNoContent()
                        } else {
                            val error =
                                Error(
                                    detail = "Group with ID $groupId not found",
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
