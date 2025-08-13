package io.protopie.cloud.scim.sp.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.protopie.cloud.scim.sp.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GroupServiceTest {
    private lateinit var userService: UserService
    private lateinit var groupService: GroupService
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setup() {
        userService = UserService()
        groupService = GroupService(userService)
    }

    @Test
    fun `그룹 생성 및 조회 테스트`() {
        // given
        val group =
            Group(
                displayName = "테스트 그룹",
            )

        // when
        val createdGroup = groupService.createGroup(group)

        // then
        assertNotNull(createdGroup.id)
        assertEquals("테스트 그룹", createdGroup.displayName)
        assertNotNull(createdGroup.meta)
        assertEquals("Group", createdGroup.meta?.resourceType)

        // 조회 확인
        val retrievedGroup = groupService.getGroupById(createdGroup.id!!)
        assertNotNull(retrievedGroup)
        assertEquals(createdGroup.id, retrievedGroup?.id)
        assertEquals(createdGroup.displayName, retrievedGroup?.displayName)
    }

    @Test
    fun `그룹에 멤버 추가 테스트`() {
        // given - 사용자 생성
        val user = userService.createUser(User(userName = "member@example.com"))

        // 그룹 생성
        val group =
            Group(
                displayName = "멤버 테스트 그룹",
                members =
                    listOf(
                        Member(value = user.id!!),
                    ),
            )

        // when
        val createdGroup = groupService.createGroup(group)

        // then
        assertNotNull(createdGroup.members)
        assertEquals(1, createdGroup.members?.size)
        assertEquals(user.id, createdGroup.members?.get(0)?.value)
        assertEquals(user.userName, createdGroup.members?.get(0)?.display)
    }

    @Test
    fun `중복된 그룹 생성 시 예외 발생 테스트`() {
        // given
        val group1 = Group(displayName = "중복 그룹")
        groupService.createGroup(group1)

        // when & then
        val group2 = Group(displayName = "중복 그룹")
        val exception =
            assertThrows<IllegalStateException> {
                groupService.createGroup(group2)
            }
        assertTrue(exception.message?.contains("already exists") ?: false)
    }

    @Test
    fun `그룹 목록 조회 및 페이지네이션 테스트`() {
        // given - 5개의 그룹 생성
        for (i in 1..5) {
            groupService.createGroup(Group(displayName = "그룹 $i"))
        }

        // when - 페이지네이션 적용하여 조회
        val page1 = groupService.getGroups(startIndex = 1, count = 2)
        val page2 = groupService.getGroups(startIndex = 3, count = 2)
        val page3 = groupService.getGroups(startIndex = 5, count = 2)

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
    fun `그룹 업데이트 테스트`() {
        // given
        val group = Group(displayName = "업데이트 테스트 그룹")
        val createdGroup = groupService.createGroup(group)

        // when
        val updatedGroup =
            groupService.updateGroup(
                createdGroup.id!!,
                group.copy(displayName = "Updated Group"),
            )

        // then
        assertNotNull(updatedGroup)
        assertEquals("Updated Group", updatedGroup?.displayName)
        assertEquals(createdGroup.id, updatedGroup?.id)
    }

    @Test
    fun `존재하지 않는 그룹 업데이트 테스트`() {
        // when
        val result = groupService.updateGroup("non-existent-id", Group(displayName = "test"))

        // then
        assertNull(result)
    }

    @Test
    fun `그룹 패치로 멤버 추가 테스트`() {
        // given - 그룹과 사용자 생성
        val group = groupService.createGroup(Group(displayName = "패치 테스트 그룹"))
        val user = userService.createUser(User(userName = "patchmember@example.com"))

        // Patch 연산 생성 - 멤버 추가
        val memberNode = objectMapper.createObjectNode()
        memberNode.put("value", user.id)

        val patchOp =
            PatchOp(
                operations =
                    listOf(
                        Operation(
                            op = "add",
                            path = "members",
                            value = memberNode,
                        ),
                    ),
            )

        // when
        val patchedGroup = groupService.patchGroup(group.id!!, patchOp)

        // then
        assertNotNull(patchedGroup)
        assertNotNull(patchedGroup?.members)
        assertEquals(1, patchedGroup?.members?.size)
        assertEquals(user.id, patchedGroup?.members?.get(0)?.value)
    }

    @Test
    fun `그룹 패치로 멤버 제거 테스트`() {
        // given - 멤버가 있는 그룹 생성
        val user = userService.createUser(User(userName = "removemember@example.com"))
        val group =
            groupService.createGroup(
                Group(
                    displayName = "멤버 제거 테스트",
                    members = listOf(Member(value = user.id!!)),
                ),
            )

        // Patch 연산 생성 - 멤버 제거
        val patchOp =
            PatchOp(
                operations =
                    listOf(
                        Operation(
                            op = "remove",
                            path = "members[value eq \"${user.id}\"]",
                        ),
                    ),
            )

        // when
        val patchedGroup = groupService.patchGroup(group.id!!, patchOp)

        // then
        assertNotNull(patchedGroup)
        assertTrue(patchedGroup?.members.isNullOrEmpty())
    }

    @Test
    fun `그룹 삭제 테스트`() {
        // given
        val group = Group(displayName = "삭제 테스트 그룹")
        val createdGroup = groupService.createGroup(group)

        // when
        val deleteResult = groupService.deleteGroup(createdGroup.id!!)

        // then
        assertTrue(deleteResult)
        assertNull(groupService.getGroupById(createdGroup.id!!))
    }

    @Test
    fun `존재하지 않는 그룹 삭제 테스트`() {
        // when
        val deleteResult = groupService.deleteGroup("non-existent-id")

        // then
        assertFalse(deleteResult)
    }
}
