package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.util.DatabaseUtil
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class CloudStorageIntegrationTest: BaseTest() {

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
            DatabaseUtil.insertUserInDb(testContext.email)
            testContext.userId = DatabaseUtil.getUserIdForEmail(testContext.email)
                    ?: fail("Missing user with email: ${testContext.email}")
        }
        suppose("Organization exists") {
            DatabaseUtil.insertOrganizationInDb(testContext.organizationName, testContext.userId)
            testContext.organizationId = DatabaseUtil.getOrganizationIdForName(testContext.organizationName)
                    ?: fail("Missing organization with name: ${testContext.organizationName}")
        }
        suppose("User is an admin of the organization") {
            DatabaseUtil.insertOrganizationMembershipInDb(testContext.userId, testContext.organizationId)
        }
        suppose("User has token") {
            testContext.token = BackendService.getJwtToken(testContext.email, DatabaseUtil.defaultUserPassword)
        }

        verify("User can upload document for organization") {
            val documentResponse = BackendService
                    .addDocument(testContext.token, testContext.organizationId, testContext.fileLocation, "test-file.json")
            testContext.documentLink = documentResponse.link
        }
        verify("Document can be fetched from Cloud storage") {
            val downloadedData = getFileDataFromCloudStorage(testContext.documentLink)
            val fileData = File(testContext.fileLocation).readText()
            assertEquals(fileData, downloadedData)
        }
        // maybe add deleting file from cloud storage
    }

    private fun getFileDataFromCloudStorage(link: String): String {
        val response = Fuel.get(link).response()
        if (response.second.statusCode != 200) fail("Could not download the file from Cloud storage. Link: $link")
        return String(response.third.get())
    }

    private class TestContext {
        val email = "test@email.com"
        val organizationName = "Das Organization"
        val fileLocation = "src/test/resources/file.json"
        var userId = -1
        var organizationId = -1
        lateinit var token: String
        lateinit var documentLink: String
    }
}
