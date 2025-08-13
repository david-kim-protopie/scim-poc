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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiscoveryApiTest {
    private val objectMapper = jacksonObjectMapper()

    private fun Application.configureTestApp(discoveryApi: DiscoveryApi) {
        install(Authentication) {
            basic("scimAuth") {
                validate { credentials ->
                    io.ktor.server.auth
                        .UserIdPrincipal(credentials.name)
                }
            }
        }
        install(ContentNegotiation) {
            jackson {}
        }
        discoveryApi.apply { configureDiscoveryApi() }
    }

    @Test
    fun `서비스 제공자 설정 조회 API 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            val response =
                client.get("/scim/v2/ServiceProviderConfig") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"))
            assertTrue(responseBody.contains("patch"))
            assertTrue(responseBody.contains("bulk"))
            assertTrue(responseBody.contains("filter"))
            assertTrue(responseBody.contains("changePassword"))
            assertTrue(responseBody.contains("sort"))
            assertTrue(responseBody.contains("etag"))
        }

    @Test
    fun `스키마 목록 조회 API 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            val response =
                client.get("/scim/v2/Schemas") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:Group"))
            assertTrue(responseBody.contains("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
        }

    @Test
    fun `특정 스키마 조회 API 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            val encodedSchema = java.net.URLEncoder.encode("urn:ietf:params:scim:schemas:core:2.0:User", "UTF-8")
            val response =
                client.get("/scim/v2/Schemas/$encodedSchema") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
            assertTrue(responseBody.contains("userName"))
            assertTrue(responseBody.contains("name"))
            assertTrue(responseBody.contains("emails"))
        }

    @Test
    fun `특정 스키마 속성 검증 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            // User 스키마 검증
            val userSchemaUrn = java.net.URLEncoder.encode("urn:ietf:params:scim:schemas:core:2.0:User", "UTF-8")
            val userResponse =
                client.get("/scim/v2/Schemas/$userSchemaUrn") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, userResponse.status)
            val userResponseBody = userResponse.bodyAsText()

            // 필수 속성 확인
            assertTrue(userResponseBody.contains("\"name\":\"userName\""))
            assertTrue(userResponseBody.contains("\"required\":true"))

            // 복합 속성 확인
            assertTrue(userResponseBody.contains("\"type\":\"complex\""))
            assertTrue(userResponseBody.contains("\"subAttributes\""))

            // Group 스키마 검증
            val groupSchemaUrn = java.net.URLEncoder.encode("urn:ietf:params:scim:schemas:core:2.0:Group", "UTF-8")
            val groupResponse =
                client.get("/scim/v2/Schemas/$groupSchemaUrn") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, groupResponse.status)
            val groupResponseBody = groupResponse.bodyAsText()

            // displayName 속성 확인
            assertTrue(groupResponseBody.contains("\"name\":\"displayName\""))
            assertTrue(groupResponseBody.contains("\"required\":true"))

            // members 속성 확인
            assertTrue(groupResponseBody.contains("\"name\":\"members\""))
            assertTrue(groupResponseBody.contains("\"multiValued\":true"))
        }

    @Test
    fun `존재하지 않는 스키마 조회 API 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            val encodedSchema = java.net.URLEncoder.encode("non-existent-schema", "UTF-8")
            val response =
                client.get("/scim/v2/Schemas/$encodedSchema") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `리소스 타입 목록 조회 API 테스트`() =
        testApplication {
            val discoveryApi = DiscoveryApi()
            application {
                configureTestApp(discoveryApi)
            }

            val response =
                client.get("/scim/v2/ResourceTypes") {
                    basicAuth("testuser", "password")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("User"))
            assertTrue(responseBody.contains("Group"))
            assertTrue(responseBody.contains("/Users"))
            assertTrue(responseBody.contains("/Groups"))
            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
            assertTrue(responseBody.contains("urn:ietf:params:scim:schemas:core:2.0:Group"))
        }
}
