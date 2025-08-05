package io.protopie.cloud.scim.sp.apis

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.protopie.cloud.scim.sp.models.Error
import io.protopie.cloud.scim.sp.models.ListResponse
import io.protopie.cloud.scim.sp.models.ResourceType
import io.protopie.cloud.scim.sp.models.ServiceProviderConfig
import io.protopie.cloud.scim.sp.service.SchemaService
import io.protopie.cloud.scim.sp.utils.respondScim

/**
 * SCIM Discovery API 라우팅 설정
 */
class DiscoveryApi {
    private val schemaService = SchemaService()

    /**
     * Discovery API 라우팅 설정
     */
    fun Application.configureDiscoveryApi() {
        routing {
            authenticate("scimAuth") {
                route("/scim/v2") {
                    // 서비스 제공자 설정 조회
                    get("/ServiceProviderConfig") {
                        val config = ServiceProviderConfig()
                        call.respondScim(HttpStatusCode.OK, config)
                    }

                    // 스키마 목록 조회
                    get("/Schemas") {
                        val schemas = schemaService.getSchemas()
                        val response =
                            ListResponse(
                                schemas = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                                totalResults = schemas.size,
                                resources = schemas,
                            )
                        call.respondScim(HttpStatusCode.OK, response)
                    }

                    // 특정 스키마 조회
                    get("/Schemas/{schemaId}") {
                        val schemaId =
                            call.parameters["schemaId"]
                                ?: throw BadRequestException("Missing schemaId parameter")

                        // URL 인코딩된 스키마 ID 처리
                        val decodedSchemaId = java.net.URLDecoder.decode(schemaId, "UTF-8")

                        val schema = schemaService.getSchemaById(decodedSchemaId)
                        if (schema != null) {
                            call.respondScim(HttpStatusCode.OK, schema)
                        } else {
                            val error =
                                Error(
                                    detail = "Schema with ID $decodedSchemaId not found",
                                    status = "404",
                                )
                            call.respondScim(HttpStatusCode.NotFound, error)
                        }
                    }

                    // 리소스 타입 목록 조회
                    get("/ResourceTypes") {
                        val userResourceType =
                            ResourceType(
                                schemas = listOf("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                                id = "User",
                                name = "User",
                                endpoint = "/Users",
                                description = "User Account",
                                schema = "urn:ietf:params:scim:schemas:core:2.0:User",
                            )

                        val groupResourceType =
                            ResourceType(
                                schemas = listOf("urn:ietf:params:scim:schemas:core:2.0:ResourceType"),
                                id = "Group",
                                name = "Group",
                                endpoint = "/Groups",
                                description = "Group",
                                schema = "urn:ietf:params:scim:schemas:core:2.0:Group",
                            )

                        val resourceTypes = listOf(userResourceType, groupResourceType)
                        val response =
                            ListResponse(
                                schemas = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                                totalResults = resourceTypes.size,
                                resources = resourceTypes,
                            )
                        call.respondScim(HttpStatusCode.OK, response)
                    }
                }
            }
        }
    }
}
