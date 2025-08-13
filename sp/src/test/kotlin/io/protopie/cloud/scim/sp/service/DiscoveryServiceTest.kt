package io.protopie.cloud.scim.sp.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiscoveryServiceTest {
    private lateinit var discoveryService: DiscoveryService
    private lateinit var schemaService: SchemaService

    @BeforeEach
    fun setup() {
        discoveryService = DiscoveryService()
        schemaService = SchemaService()
    }

    @Test
    fun `서비스 제공자 설정 조회 테스트`() {
        // when
        val config = discoveryService.getServiceProviderConfig()

        // then
        assertNotNull(config)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig", config.schemas?.firstOrNull())
        assertNotNull(config.patch)
        assertNotNull(config.bulk)
        assertNotNull(config.filter)
        assertNotNull(config.changePassword)
        assertNotNull(config.sort)
        assertNotNull(config.etag)
    }

    @Test
    fun `스키마 목록 조회 테스트`() {
        // when
        val schemas = discoveryService.getSchemas()

        // then
        assertNotNull(schemas)
        assertEquals(2, schemas.totalResults)
        assertEquals(2, schemas.resources.size)

        // User 스키마와 Group 스키마가 있는지 확인
        val schemaIds = schemas.resources.map { it.id }
        assertTrue(schemaIds.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
        assertTrue(schemaIds.contains("urn:ietf:params:scim:schemas:core:2.0:Group"))

        // SchemaService와 결과 비교
        val schemaServiceResults = schemaService.getSchemas()
        assertEquals(schemaServiceResults.size, schemas.resources.size)
        assertTrue(schemaServiceResults.map { it.id }.containsAll(schemaIds))
    }

    @Test
    fun `User 스키마 조회 테스트`() {
        // when
        val userSchema = discoveryService.getSchemaByUrn("urn:ietf:params:scim:schemas:core:2.0:User")

        // then
        assertNotNull(userSchema)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:User", userSchema?.id)
        assertEquals("User", userSchema?.name)

        // 필수 속성 확인
        val requiredAttributes = userSchema?.attributes?.filter { it.required == true }
        assertNotNull(requiredAttributes)
        assertTrue(requiredAttributes!!.any { it.name == "userName" })

        // SchemaService와 결과 비교
        val schemaServiceUser = schemaService.getSchemaById("urn:ietf:params:scim:schemas:core:2.0:User")
        assertEquals(schemaServiceUser?.id, userSchema?.id)
        assertEquals(schemaServiceUser?.name, userSchema?.name)
    }

    @Test
    fun `Group 스키마 조회 테스트`() {
        // when
        val groupSchema = discoveryService.getSchemaByUrn("urn:ietf:params:scim:schemas:core:2.0:Group")

        // then
        assertNotNull(groupSchema)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:Group", groupSchema?.id)
        assertEquals("Group", groupSchema?.name)

        // 필수 속성 확인
        val requiredAttributes = groupSchema?.attributes?.filter { it.required == true }
        assertNotNull(requiredAttributes)
        assertTrue(requiredAttributes!!.any { it.name == "displayName" })

        // members 속성 확인
        val membersAttribute = groupSchema?.attributes?.find { it.name == "members" }
        assertNotNull(membersAttribute)
        assertTrue(membersAttribute?.multiValued == true)
        assertEquals("complex", membersAttribute?.type)

        // SchemaService와 결과 비교
        val schemaServiceGroup = schemaService.getSchemaById("urn:ietf:params:scim:schemas:core:2.0:Group")
        assertEquals(schemaServiceGroup?.id, groupSchema?.id)
        assertEquals(schemaServiceGroup?.name, groupSchema?.name)
    }

    @Test
    fun `존재하지 않는 스키마 조회 테스트`() {
        // when
        val nonExistentSchema = discoveryService.getSchemaByUrn("non-existent-schema")

        // then
        assertNull(nonExistentSchema)

        // SchemaService와 결과 비교
        val schemaServiceResult = schemaService.getSchemaById("non-existent-schema")
        assertNull(schemaServiceResult)
    }

    @Test
    fun `리소스 타입 목록 조회 테스트`() {
        // when
        val resourceTypes = discoveryService.getResourceTypes()

        // then
        assertNotNull(resourceTypes)
        assertEquals(2, resourceTypes.totalResults)
        assertEquals(2, resourceTypes.resources.size)

        // User와 Group 리소스 타입이 있는지 확인
        val resourceTypeIds = resourceTypes.resources.map { it.id }
        assertTrue(resourceTypeIds.contains("User"))
        assertTrue(resourceTypeIds.contains("Group"))

        // User 리소스 타입 상세 확인
        val userResourceType = resourceTypes.resources.find { it.id == "User" }
        assertNotNull(userResourceType)
        assertEquals("/Users", userResourceType?.endpoint)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:User", userResourceType?.schema)

        // Group 리소스 타입 상세 확인
        val groupResourceType = resourceTypes.resources.find { it.id == "Group" }
        assertNotNull(groupResourceType)
        assertEquals("/Groups", groupResourceType?.endpoint)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:Group", groupResourceType?.schema)
    }
}
