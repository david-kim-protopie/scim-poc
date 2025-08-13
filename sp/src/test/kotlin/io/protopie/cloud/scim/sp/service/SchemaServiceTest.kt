package io.protopie.cloud.scim.sp.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SchemaServiceTest {
    private lateinit var schemaService: SchemaService

    @BeforeEach
    fun setup() {
        schemaService = SchemaService()
    }

    @Test
    fun `모든 스키마 목록 조회 테스트`() {
        // when
        val schemas = schemaService.getSchemas()

        // then
        assertNotNull(schemas)
        assertEquals(2, schemas.size)

        // User 스키마와 Group 스키마가 있는지 확인
        val schemaIds = schemas.map { it.id }
        assertTrue(schemaIds.contains("urn:ietf:params:scim:schemas:core:2.0:User"))
        assertTrue(schemaIds.contains("urn:ietf:params:scim:schemas:core:2.0:Group"))
    }

    @Test
    fun `User 스키마 조회 테스트`() {
        // when
        val userSchema = schemaService.getSchemaById("urn:ietf:params:scim:schemas:core:2.0:User")

        // then
        assertNotNull(userSchema)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:User", userSchema?.id)
        assertEquals("User", userSchema?.name)
        assertEquals("User Account", userSchema?.description)

        // 필수 속성 확인
        val userAttributes = userSchema?.attributes
        assertNotNull(userAttributes)

        // userName 속성이 필수인지 확인
        val userNameAttribute = userAttributes?.find { it.name == "userName" }
        assertNotNull(userNameAttribute)
        assertTrue(userNameAttribute?.required == true)
        assertEquals("string", userNameAttribute?.type)

        // name 속성 확인
        val nameAttribute = userAttributes?.find { it.name == "name" }
        assertNotNull(nameAttribute)
        assertEquals("complex", nameAttribute?.type)
        assertTrue(nameAttribute?.multiValued == false)

        // name의 하위 속성 확인
        val nameSubAttributes = nameAttribute?.subAttributes
        assertNotNull(nameSubAttributes)
        assertTrue(nameSubAttributes?.size!! >= 6) // 6개 이상의 하위 속성

        // emails 속성 확인
        val emailsAttribute = userAttributes?.find { it.name == "emails" }
        assertNotNull(emailsAttribute)
        assertEquals("complex", emailsAttribute?.type)
        assertTrue(emailsAttribute?.multiValued == true)

        // emails의 하위 속성 확인
        val emailsSubAttributes = emailsAttribute?.subAttributes
        assertNotNull(emailsSubAttributes)
        assertTrue(emailsSubAttributes?.any { it.name == "value" } == true)
        assertTrue(emailsSubAttributes?.any { it.name == "type" } == true)
        assertTrue(emailsSubAttributes?.any { it.name == "primary" } == true)

        // meta 확인
        assertNotNull(userSchema?.meta)
        assertEquals("Schema", userSchema?.meta?.resourceType)
    }

    @Test
    fun `Group 스키마 조회 테스트`() {
        // when
        val groupSchema = schemaService.getSchemaById("urn:ietf:params:scim:schemas:core:2.0:Group")

        // then
        assertNotNull(groupSchema)
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:Group", groupSchema?.id)
        assertEquals("Group", groupSchema?.name)

        // 속성 확인
        val groupAttributes = groupSchema?.attributes
        assertNotNull(groupAttributes)

        // displayName 속성이 필수인지 확인
        val displayNameAttribute = groupAttributes?.find { it.name == "displayName" }
        assertNotNull(displayNameAttribute)
        assertTrue(displayNameAttribute?.required == true)
        assertEquals("string", displayNameAttribute?.type)

        // members 속성 확인
        val membersAttribute = groupAttributes?.find { it.name == "members" }
        assertNotNull(membersAttribute)
        assertEquals("complex", membersAttribute?.type)
        assertTrue(membersAttribute?.multiValued == true)

        // members의 하위 속성 확인
        val membersSubAttributes = membersAttribute?.subAttributes
        assertNotNull(membersSubAttributes)
        assertTrue(membersSubAttributes?.any { it.name == "value" } == true)
        assertTrue(membersSubAttributes?.any { it.name == "ref" } == true)
        assertTrue(membersSubAttributes?.any { it.name == "display" } == true)
        assertTrue(membersSubAttributes?.any { it.name == "type" } == true)

        // type 속성의 canonicalValues 확인
        val typeAttribute = membersSubAttributes?.find { it.name == "type" }
        assertNotNull(typeAttribute)
        val canonicalValues = typeAttribute?.canonicalValues
        assertNotNull(canonicalValues)
        assertTrue(canonicalValues?.contains("User") == true)
        assertTrue(canonicalValues?.contains("Group") == true)

        // meta 확인
        assertNotNull(groupSchema?.meta)
        assertEquals("Schema", groupSchema?.meta?.resourceType)
    }

    @Test
    fun `존재하지 않는 스키마 조회 테스트`() {
        // when
        val nonExistentSchema = schemaService.getSchemaById("non-existent-schema")

        // then
        assertNull(nonExistentSchema)
    }
}
