package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.protopie.cloud.scim.sp.database.DatabaseFactory
import io.protopie.cloud.scim.sp.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {
    private lateinit var userService: UserService
    private val objectMapper = ObjectMapper()

    @BeforeAll
    fun setupDatabase() {
        // 테스트용 데이터베이스 초기화
        DatabaseFactory.initForTesting()
    }

    @BeforeEach
    fun setup() {
        userService = UserService()
        // 테스트 시작 전 데이터 초기화
        userService.clearAllUsers()
    }

    @Test
    fun `사용자 생성 및 조회 테스트`() {
        // given
        val user =
            User(
                userName = "testuser@example.com",
                name =
                    Name(
                        givenName = "홍",
                        familyName = "길동",
                    ),
                emails =
                    listOf(
                        ComplexAttribute(
                            value = "testuser@example.com",
                            primary = true,
                        ),
                    ),
                active = true,
            )

        // when
        val createdUser = userService.createUser(user)

        // then
        assertNotNull(createdUser.id)
        assertEquals("testuser@example.com", createdUser.userName)
        assertEquals("홍", createdUser.name?.givenName)
        assertEquals("길동", createdUser.name?.familyName)
        assertTrue(createdUser.active ?: false)

        // 조회 확인
        val retrievedUser = userService.getUserById(createdUser.id!!)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser?.id)
        assertEquals(createdUser.userName, retrievedUser?.userName)
    }

    @Test
    fun `중복된 사용자 생성 시 예외 발생 테스트`() {
        // given
        val user1 = User(userName = "duplicate@example.com")
        userService.createUser(user1)

        // when & then
        val user2 = User(userName = "duplicate@example.com")
        val exception =
            assertThrows<IllegalStateException> {
                userService.createUser(user2)
            }
        assertTrue(exception.message?.contains("already exists") ?: false)
    }

    @Test
    fun `사용자 목록 조회 및 페이지네이션 테스트`() {
        // given - 5명의 사용자 생성
        for (i in 1..5) {
            userService.createUser(User(userName = "user$i@example.com"))
        }

        // when - 페이지네이션 적용하여 조회
        val page1 = userService.getUsers(startIndex = 1, count = 2)
        val page2 = userService.getUsers(startIndex = 3, count = 2)
        val page3 = userService.getUsers(startIndex = 5, count = 2)

        // then
        assertEquals(5, page1.totalResults) // 전체 결과 수는 5
        assertEquals(2, page1.itemsPerPage) // 첫 페이지는 2개
        assertEquals(2, page2.itemsPerPage) // 두번째 페이지는 2개
        assertEquals(1, page3.itemsPerPage) // 세번째 페이지는 1개

        assertEquals(1, page1.startIndex)
        assertEquals(3, page2.startIndex)
        assertEquals(5, page3.startIndex)
    }

    @Test
    fun `사용자 업데이트 테스트`() {
        // given
        val user = User(userName = "updatetest@example.com")
        val createdUser = userService.createUser(user)

        // when
        val updatedUser =
            userService.updateUser(
                createdUser.id!!,
                user.copy(displayName = "Updated User"),
            )

        // then
        assertNotNull(updatedUser)
        assertEquals("Updated User", updatedUser?.displayName)
        assertEquals(createdUser.id, updatedUser?.id)
    }

    @Test
    fun `존재하지 않는 사용자 업데이트 테스트`() {
        // when
        val result = userService.updateUser("non-existent-id", User(userName = "test"))

        // then
        assertNull(result)
    }

    @Test
    fun `사용자 패치 테스트`() {
        // given
        val user = User(userName = "patchtest@example.com")
        val createdUser = userService.createUser(user)

        // Patch 연산 생성
        val patchValue = objectMapper.createObjectNode()
        patchValue.put("active", false)
        patchValue.put("displayName", "Patched User")

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

        // when
        val patchedUser = userService.patchUser(createdUser.id!!, patchOp)

        // then
        assertNotNull(patchedUser)
        assertEquals("Patched User", patchedUser?.displayName)
        assertFalse(patchedUser?.active ?: true)
    }

    @Test
    fun `사용자 삭제 테스트`() {
        // given
        val user = User(userName = "deletetest@example.com")
        val createdUser = userService.createUser(user)

        // when
        val deleteResult = userService.deleteUser(createdUser.id!!)

        // then
        assertTrue(deleteResult)
        assertNull(userService.getUserById(createdUser.id!!))
    }

    @Test
    fun `존재하지 않는 사용자 삭제 테스트`() {
        // when
        val deleteResult = userService.deleteUser("non-existent-id")

        // then
        assertFalse(deleteResult)
    }
}
