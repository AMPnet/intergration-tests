package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.backend.UserService
import com.ampnet.integration.tests.util.DatabaseUtil
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class UserServiceIntegrationTest : BaseTest() {

    private val testContext = TestContext()

    @Test
    fun mustReturnUserDataToCrowdfudningBackend() {
        suppose("The user exists") {
            DatabaseUtil.insertUserInDb(testContext.email, testContext.uuid)
        }
        suppose("Organization exists") {
            DatabaseUtil.insertOrganizationInDb(testContext.organizationName, testContext.uuid)
            testContext.organizationId = DatabaseUtil.getOrganizationIdForName(testContext.organizationName)
                    ?: fail("Missing organization with name: ${testContext.organizationName}")
        }
        suppose("User is a member of the organization") {
            DatabaseUtil.insertOrganizationMembershipInDb(testContext.uuid, testContext.organizationId)
        }

        verify("User can get token") {
            testContext.token = UserService.getJwtToken(testContext.email, DatabaseUtil.defaultUserPassword)
        }
        verify("User can get organization memberships") {
            val members = BackendService.getOrganizationMemberships(testContext.token, testContext.organizationId)
            assertEquals(1, members.members.size)
            val member = members.members[0]
            assertEquals("first", member.firstName)
            assertEquals("last", member.lastName)
            assertEquals(testContext.uuid, member.uuid)
            assertEquals("ORG_ADMIN", member.role)
        }
    }

    private class TestContext {
        val email: String = "user@mail.com"
        lateinit var token: String
        val uuid: UUID = UUID.randomUUID()
        val organizationName = "Organization"
        var organizationId = -1
    }
}
