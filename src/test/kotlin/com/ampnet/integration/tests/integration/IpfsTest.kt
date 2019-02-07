package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.util.DatabaseUtil
import io.ipfs.api.IPFS
import io.ipfs.multihash.Multihash
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class IpfsTest: BaseTest() {

    private lateinit var testContext: TestContext

    @BeforeTest
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddAndGetOrganizationDocument() {
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
            testContext.documentHash = documentResponse.hash
        }
        verify("Document can be fetched from IPFS") {
            val data = getDataFromIpfs(testContext.documentHash)
            assertNotNull(data)
            val fileData = File(testContext.fileLocation).readText()
            assertEquals(fileData, String(data))
        }
    }

    private fun getDataFromIpfs(hash: String): ByteArray? {
        val ipfs = IPFS("/ip4/127.0.0.1/tcp/5001")
        ipfs.refs.local()

        val filePointer = Multihash.fromBase58(hash)
        val future = Executors.newCachedThreadPool().submit<ByteArray> { ipfs.cat(filePointer) }
        return try {
            val fileContents = future.get(3000, TimeUnit.MILLISECONDS)
            fileContents
        } catch (ex: Exception) {
            null
        }
    }

    private class TestContext {
        val email = "test@email.com"
        val organizationName = "Das Organization"
        val fileLocation = "src/test/resources/file.json"
        var userId = -1
        var organizationId = -1
        lateinit var token: String
        lateinit var documentHash: String
    }
}
