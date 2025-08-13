package io.protopie.cloud.scim.sp.apis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.protopie.cloud.scim.sp.database.Users
import io.protopie.cloud.scim.sp.models.*
import io.protopie.cloud.scim.sp.service.UserService
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GroupsApiTest {
    private val objectMapper =
        jacksonObjectMapper().apply {
            // 역직렬화 실패 방지 설정
            configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 타입 정보 처리 개선
            findAndRegisterModules()
        }

    private fun Application.configureTestApp(
        usersApi: UsersApi,
        groupsApi: GroupsApi,
    ) {
        // 테스트 시작 전 사용자 및 그룹 데이터 초기화
        usersApi.getUserService().clearAllUsers()
        groupsApi.getGroupService().clearAllGroups()
        install(Authentication) {
            basic("scimAuth") {
                validate { credentials ->
                    UserIdPrincipal(credentials.name)
                }
            }
        }
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
        usersApi.apply { configureUsersApi() }
        groupsApi.apply { configureGroupsApi() }
    }

    @Test
    fun `그룹 생성 및 조회 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 그룹 생성
            val group = Group(displayName = "API 테스트 그룹")
            val groupJson = objectMapper.writeValueAsString(group)

            val createResponse =
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(groupJson)
                }

            assertEquals(HttpStatusCode.Created, createResponse.status)
            val responseBody = createResponse.bodyAsText()
            println("Response body: $responseBody")
            val createdGroup = objectMapper.readValue(responseBody, Group::class.java)
            assertNotNull(createdGroup.id)
            assertEquals("API 테스트 그룹", createdGroup.displayName)

            // 그룹 조회
            val getResponse =
                client.get("/scim/v2/Groups/${createdGroup.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            val retrieveResponseBody = getResponse.bodyAsText()
            val retrievedGroup = objectMapper.readValue(retrieveResponseBody, Group::class.java)
            assertEquals(createdGroup.id, retrievedGroup.id)
        }

    @Test
    fun `멤버가 있는 그룹 생성 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 테스트 사용자 생성
            val user = User(userName = "groupmember@example.com")
            val userResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }

            assertEquals(HttpStatusCode.Created, userResponse.status)
            val userResponseBody = userResponse.bodyAsText()
            println("User response body: $userResponseBody")
            val createdUser = objectMapper.readValue(userResponseBody, User::class.java)

            // 멤버가 있는 그룹 생성
            val group =
                Group(
                    displayName = "멤버 테스트 그룹",
                    members =
                        listOf(
                            Member(value = createdUser.id!!),
                        ),
                )

            val groupResponse =
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(group))
                }

            assertEquals(HttpStatusCode.Created, groupResponse.status)
            val groupResponseBody = groupResponse.bodyAsText()
            println("Group with member response: $groupResponseBody")
            val createdGroup = objectMapper.readValue(groupResponseBody, Group::class.java)

            assertNotNull(createdGroup.members)
            assertEquals(1, createdGroup.members?.size)
            assertEquals(createdUser.id, createdGroup.members?.get(0)?.value)
            assertEquals(createdUser.userName, createdGroup.members?.get(0)?.display)
        }

    @Test
    fun `그룹 목록 조회 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 테스트 그룹 생성
            for (i in 1..3) {
                val group = Group(displayName = "API 목록 테스트 그룹 $i")
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(group))
                }
            }

            // 그룹 목록 조회
            val getResponse =
                client.get("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            val responseBody = getResponse.bodyAsText()
            assertTrue(responseBody.contains("API 목록 테스트 그룹 1"))
            assertTrue(responseBody.contains("API 목록 테스트 그룹 2"))
            assertTrue(responseBody.contains("API 목록 테스트 그룹 3"))
        }

    @Test
    fun `그룹 업데이트 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 그룹 생성
            val group = Group(displayName = "업데이트 테스트 그룹")
            val createResponse =
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(group))
                }

            val createdGroup = objectMapper.readValue(createResponse.bodyAsText(), Group::class.java)

            // 그룹 업데이트
            val updatedGroup = createdGroup.copy(displayName = "Updated API Group")
            val updateResponse =
                client.put("/scim/v2/Groups/${createdGroup.id}") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(updatedGroup))
                }

            assertEquals(HttpStatusCode.OK, updateResponse.status)
            val updateResponseBody = updateResponse.bodyAsText()
            val retrievedGroup = objectMapper.readValue(updateResponseBody, Group::class.java)
            assertEquals("Updated API Group", retrievedGroup.displayName)
        }

    @Test
    fun `그룹 패치로 멤버 추가 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 그룹 생성
            val group = Group(displayName = "패치 테스트 그룹")
            val createGroupResponse =
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(group))
                }

            val createdGroup = objectMapper.readValue(createGroupResponse.bodyAsText(), Group::class.java)

            // 사용자 생성
            val user = User(userName = "patchgroupmember@example.com")
            val createUserResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }

            val createdUser = objectMapper.readValue(createUserResponse.bodyAsText(), User::class.java)

            // 패치 요청 준비 - 멤버 추가
            val member = Member(value = createdUser.id!!)
            val memberJson = objectMapper.writeValueAsString(member)

            val patchOp =
                PatchOp(
                    operations =
                        listOf(
                            Operation(
                                op = "add",
                                path = "members",
                                value = objectMapper.readTree(memberJson),
                            ),
                        ),
                )

            // 그룹 패치
            val patchResponse =
                client.patch("/scim/v2/Groups/${createdGroup.id}") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(patchOp))
                }

            assertEquals(HttpStatusCode.OK, patchResponse.status)
            val patchResponseBody = patchResponse.bodyAsText()
            println("Patched group response: $patchResponseBody")
            val patchedGroup = objectMapper.readValue(patchResponseBody, Group::class.java)

            assertNotNull(patchedGroup.members)
            assertEquals(1, patchedGroup.members?.size)
            assertEquals(createdUser.id, patchedGroup.members?.get(0)?.value)
        }

    @Test
    fun `그룹 삭제 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            val groupsApi = GroupsApi(usersApi.getUserService())

            application {
                configureTestApp(usersApi, groupsApi)
            }

            // 그룹 생성
            val group = Group(displayName = "삭제 테스트 그룹")
            val createResponse =
                client.post("/scim/v2/Groups") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(group))
                }

            val createdGroup = objectMapper.readValue(createResponse.bodyAsText(), Group::class.java)

            // 그룹 삭제
            val deleteResponse =
                client.delete("/scim/v2/Groups/${createdGroup.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // 삭제 확인
            val getResponse =
                client.get("/scim/v2/Groups/${createdGroup.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.NotFound, getResponse.status)
        }
}

/**
 * 모든 사용자 데이터 초기화 (테스트용)
 */
fun UserService.clearAllUsers() =
    transaction {
        Users.deleteAll()
    }

/**
 * 모든 그룹 데이터 초기화 (테스트용)
 */
fun io.protopie.cloud.scim.sp.service.GroupService.clearAllGroups() =
    transaction {
        io.protopie.cloud.scim.sp.database.Groups
            .deleteAll()
    }
