package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.backend.UserService
import com.ampnet.integration.tests.util.DatabaseUtil
import com.github.kittinunf.fuel.Fuel
import java.io.File
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class CloudStorageIntegrationTest: BaseTest() {

    private val user = TestUser()
    private lateinit var testContext: TestContext

    @BeforeTest
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddAndGetOrganizationDocument() {
        suppose("Database is clean") {
            DatabaseUtil.cleanBackendDb()
        }
        suppose("User exists in database") {
            DatabaseUtil.insertUserInDb(user.email, user.uuid)
        }
        suppose("Organization exists") {
            DatabaseUtil.insertOrganizationInDb(testContext.organizationName, user.uuid)
            testContext.organizationId = DatabaseUtil.getOrganizationIdForName(testContext.organizationName)
                    ?: fail("Missing organization with name: ${testContext.organizationName}")
        }
        suppose("User is an admin of the organization") {
            DatabaseUtil.insertOrganizationMembershipInDb(user.uuid, testContext.organizationId)
        }
        suppose("User has token") {
            testContext.token = UserService.getJwtToken(user.email, DatabaseUtil.defaultUserPassword)
        }

        verify("User can upload document for organization") {
            val documentResponse = BackendService
                    .addDocument(testContext.token, testContext.organizationId, testContext.fileLocation, "test-file.json")
            testContext.documentLink = documentResponse.link
            testContext.documentId = documentResponse.id
        }
        verify("Document can be fetched from Cloud storage") {
            val downloadedData = getFileDataFromCloudStorage(testContext.documentLink)
            val fileData = File(testContext.fileLocation).readText()
            assertEquals(fileData, downloadedData)
        }
        verify("User can delete document") {
            BackendService.removeDocument(testContext.token, testContext.organizationId, testContext.documentId)
        }
        // deleting is too slow to be tested
    }

    private fun getFileDataFromCloudStorage(link: String): String {
        val response = Fuel.get(link).response()
        if (response.second.statusCode != 200) fail("Could not download the file from Cloud storage. Link: $link")
        return String(response.third.get())
    }

    private class TestContext {
        val organizationName = "Das Organization"
        val fileLocation = "src/test/resources/file.json"
        var organizationId = -1
        lateinit var token: String
        lateinit var documentLink: String
        var documentId = -1
    }

    private class TestUser {
        val email = "test@email.com"
        val uuid = UUID.randomUUID().toString()
    }
}
