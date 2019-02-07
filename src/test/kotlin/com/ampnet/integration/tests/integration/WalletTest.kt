package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.util.BlockchainUtil
import com.ampnet.integration.tests.util.DatabaseUtil
import com.ampnet.integration.tests.backend.WalletCreateRequest
import com.ampnet.integration.tests.backend.WalletResponse
import org.junit.AfterClass
import org.junit.BeforeClass
import org.web3j.crypto.Credentials
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import java.io.File
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait

class WalletTest: BaseTest() {

    private val email = "test@email.com"
    private val organizationName = "Organization"
    private val projectName = "Project"

    private lateinit var testContext: TestContext

    @BeforeTest
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun createProject() {
        suppose("Databases are clean") {
            DatabaseUtil.cleanBlockchainDb()
            DatabaseUtil.cleanBackendDb()
        }

        verify("User can create wallet") {
            createUserWallet()
        }
        verify("Organization can create wallet") {
            createOrganizationWallet()
        }

        verify("User can approve organization") {
            val organizationResponse = BackendService.approveOrganization(testContext.token, testContext.organizationId)
            assertNotNull(organizationResponse)
        }

        suppose("Project exists") {
            DatabaseUtil.insertProjectInDb(projectName, testContext.userId, testContext.organizationId)
            testContext.projectId = DatabaseUtil.getProjectIdForName(projectName)
                    ?: fail("Missing project with name: $projectName")
        }

        verify("User can create project wallet") {
            val transactionToCreateProject = BackendService
                    .getTransactionToCreateProjectWallet(testContext.token, testContext.projectId)
            val signedTransactionToCreateProject = BlockchainUtil
                    .signTransaction(transactionToCreateProject.transactionData, BlockchainUtil.alice)
            val projectWallet = BackendService
                    .createProjectWallet(testContext.token, testContext.projectId, signedTransactionToCreateProject)
            assertNotNull(projectWallet)
        }
        verify("User can get balance for project wallet") {
            val projectWalletWithBalance = BackendService.getProjectWallet(testContext.token, testContext.projectId)
            assertNotNull(projectWalletWithBalance)
        }
    }

    private fun createUserWallet() {
        suppose("User exists in database") {
            DatabaseUtil.insertUserInDb(email)
            testContext.userId = DatabaseUtil.getUserIdForEmail(email) ?: fail("Missing user with email: $email")
        }

        verify("User can get token") {
            testContext.token = BackendService.getJwtToken(email, DatabaseUtil.defaultUserPassword)
        }
        verify("User does not have a wallet") {
            val emptyWallet = BackendService.getUserWallet(testContext.token)
            assertNull(emptyWallet)
        }
        verify("User can create a wallet") {
            val wallet = createUserWallet(testContext.token, BlockchainUtil.alice)
            assertNotNull(wallet)
        }
        verify("User can get wallet balance") {
            val walletWithBalance = BackendService.getUserWallet(testContext.token)
            assertNotNull(walletWithBalance)
        }
    }

    private fun createOrganizationWallet() {
        suppose("User has wallet") {
            val walletWithBalance = BackendService.getUserWallet(testContext.token)
            assertNotNull(walletWithBalance)
        }
        suppose("Organization exists") {
            DatabaseUtil.insertOrganizationInDb(organizationName, testContext.userId)
            testContext.organizationId = DatabaseUtil.getOrganizationIdForName(organizationName)
                    ?: fail("Missing organization with name: $organizationName")
        }

        suppose("User is an admin of the organization") {
            DatabaseUtil.insertOrganizationMembershipInDb(testContext.userId, testContext.organizationId)
        }

        verify("User can create organization wallet") {
            val transactionToCreateOrganization = BackendService
                    .getTransactionToCreateOrganizationWallet(testContext.token, testContext.organizationId)
            val signedTransaction = BlockchainUtil
                    .signTransaction(transactionToCreateOrganization.transactionData, BlockchainUtil.alice)
            BackendService.createOrganizationWallet(testContext.token, testContext.organizationId, signedTransaction)
        }
        verify("User can get balance for organization wallet") {
            Thread.sleep(3000)
            val walletWithBalance = BackendService.getOrganizationWallet(testContext.token, testContext.organizationId)
            assertNotNull(walletWithBalance)
        }
    }

    private fun createUserWallet(token: String, credentials: Credentials): WalletResponse {
        val walletToken = BackendService.getWalletToken(token)
        val walletCreateRequest = WalletCreateRequest(
                credentials.address, BlockchainUtil.getPublicKey(credentials), walletToken.token
        )
        return BackendService.createUserWallet(token, walletCreateRequest)
    }

    private class TestContext {
        lateinit var token: String
        var userId = -1
        var organizationId = -1
        var projectId = -1
    }
}
