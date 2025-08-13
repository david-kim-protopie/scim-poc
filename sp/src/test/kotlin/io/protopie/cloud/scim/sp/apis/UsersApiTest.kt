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
import io.protopie.cloud.scim.sp.models.Operation
import io.protopie.cloud.scim.sp.models.PatchOp
import io.protopie.cloud.scim.sp.models.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UsersApiTest {
    private val objectMapper =
        jacksonObjectMapper().apply {
            // 역직렬화 실패 방지 설정
            configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            // 타입 정보 처리 개선
            findAndRegisterModules()
        }

    private fun Application.configureTestApp(usersApi: UsersApi) {
        // 테스트 시작 전 데이터 초기화
        usersApi.getUserService().clearAllUsers()
        install(Authentication) {
            basic("scimAuth") {
                validate { credentials ->
                    io.ktor.server.auth
                        .UserIdPrincipal(credentials.name)
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
    }

    @Test
    fun `사용자 생성 및 조회 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            application {
                configureTestApp(usersApi)
            }

            // 사용자 생성
            val user = User(userName = "apitest@example.com")
            val userJson = objectMapper.writeValueAsString(user)

            val createResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(userJson)
                }

            assertEquals(HttpStatusCode.Created, createResponse.status)
            val responseBody = createResponse.bodyAsText()
            println("Create user response body: $responseBody")
            assertFalse(responseBody.isBlank(), "Response body should not be empty")

            // 안전한 역직렬화
            val createdUser =
                try {
                    objectMapper.readValue(responseBody, User::class.java)
                } catch (e: Exception) {
                    println("Failed to deserialize response: ${e.message}")
                    fail("Failed to deserialize response: ${e.message}")
                }
            assertNotNull(createdUser.id)
            assertEquals("apitest@example.com", createdUser.userName)

            // 사용자 조회
            val getResponse =
                client.get("/scim/v2/Users/${createdUser.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            val retrievedUser = objectMapper.readValue(getResponse.bodyAsText(), User::class.java)
            assertEquals(createdUser.id, retrievedUser.id)
        }

    @Test
    fun `사용자 목록 조회 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            application {
                configureTestApp(usersApi)
            }

            // 테스트 사용자 생성
            for (i in 1..3) {
                val user = User(userName = "listuser$i@example.com")
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }
            }

            // 사용자 목록 조회
            val getResponse =
                client.get("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            val responseBody = getResponse.bodyAsText()
            assertTrue(responseBody.contains("listuser1@example.com"))
            assertTrue(responseBody.contains("listuser2@example.com"))
            assertTrue(responseBody.contains("listuser3@example.com"))
        }

    @Test
    fun `사용자 업데이트 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            application {
                configureTestApp(usersApi)
            }

            // 사용자 생성
            val user = User(userName = "updateuser@example.com")
            val createResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }

            val createdUser = objectMapper.readValue(createResponse.bodyAsText(), User::class.java)

            // 사용자 업데이트
            val updatedUser = createdUser.copy(displayName = "Updated API User")
            val updateResponse =
                client.put("/scim/v2/Users/${createdUser.id}") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(updatedUser))
                }

            assertEquals(HttpStatusCode.OK, updateResponse.status)
            val retrievedUser = objectMapper.readValue(updateResponse.bodyAsText(), User::class.java)
            assertEquals("Updated API User", retrievedUser.displayName)
        }

    @Test
    fun `사용자 패치 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            application {
                configureTestApp(usersApi)
            }

            // 사용자 생성
            val user = User(userName = "patchuser@example.com")
            val createResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }

            val createdUser = objectMapper.readValue(createResponse.bodyAsText(), User::class.java)

            // 패치 요청 준비
            val patchValue = objectMapper.createObjectNode()
            patchValue.put("active", false)
            val patchOp =
                PatchOp(
                    operations =
                        listOf(
                            Operation(
                                op = "replace",
                                value = objectMapper.deserializationConfig.nodeFactory.textNode(patchValue.toString()),
                            ),
                        ),
                )

            // 사용자 패치
            val patchResponse =
                client.patch("/scim/v2/Users/${createdUser.id}") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(patchOp))
                }

            assertEquals(HttpStatusCode.OK, patchResponse.status)
            val patchedUser = objectMapper.readValue(patchResponse.bodyAsText(), User::class.java)
            assertFalse(patchedUser.active)
        }

    @Test
    fun `사용자 삭제 API 테스트`() =
        testApplication {
            val usersApi = UsersApi()
            application {
                configureTestApp(usersApi)
            }

            // 사용자 생성
            val user = User(userName = "deleteuser@example.com")
            val createResponse =
                client.post("/scim/v2/Users") {
                    basicAuth("testuser", "password")
                    contentType(ContentType.Application.Json)
                    setBody(objectMapper.writeValueAsString(user))
                }

            val createdUser = objectMapper.readValue(createResponse.bodyAsText(), User::class.java)

            // 사용자 삭제
            val deleteResponse =
                client.delete("/scim/v2/Users/${createdUser.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // 삭제 확인
            val getResponse =
                client.get("/scim/v2/Users/${createdUser.id}") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.NotFound, getResponse.status)
        }
}
