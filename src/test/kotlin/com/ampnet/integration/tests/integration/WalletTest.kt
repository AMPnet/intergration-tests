package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.util.BlockchainUtil
import com.ampnet.integration.tests.util.DatabaseUtil
import com.ampnet.integration.tests.backend.WalletCreateRequest
import com.ampnet.integration.tests.backend.WalletResponse
import org.web3j.crypto.Credentials
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class WalletTest: BaseTest() {

    private val alice = TestUser("alice@email.com", BlockchainUtil.alice)

    @Test
    fun createProject() {
        suppose("Databases are clean") {
            DatabaseUtil.cleanBlockchainDb()
            DatabaseUtil.cleanBackendDb()
        }

        verify("User Alice can create wallet") {
            createUserWithWallet(alice)
        }
        verify("Organization can create wallet") {
            createOrganizationWithWallet(alice, "Alice organization")
        }

        verify("User Alice can approve organization") {
            val organizationResponse = BackendService.approveOrganization(alice.token, alice.organizationId)
            assertNotNull(organizationResponse)
        }

        verify("User Alice can create project wallet") {
            createProjectWithWallet(alice, "Alice project")
        }
        verify("User Alice can get balance for project wallet") {
            val projectWalletWithBalance = BackendService.getProjectWallet(alice.token, alice.projectId)
            assertNotNull(projectWalletWithBalance)
        }
    }

    private fun createUserWithWallet(user: TestUser) {
        suppose("User exists in database") {
            DatabaseUtil.insertUserInDb(user.email)
            user.userId = DatabaseUtil.getUserIdForEmail(user.email)
                    ?: fail("Missing user with email: ${user.email}")
        }

        verify("User can get token") {
            user.token = BackendService.getJwtToken(user.email, DatabaseUtil.defaultUserPassword)
        }
        verify("User does not have a wallet") {
            val emptyWallet = BackendService.getUserWallet(user.token)
            assertNull(emptyWallet)
        }
        verify("User can create a wallet") {
            val wallet = createUserWallet(user.token, user.credentials)
            assertNotNull(wallet)
        }
        verify("User can get wallet balance") {
            val walletWithBalance = BackendService.getUserWallet(user.token)
            assertNotNull(walletWithBalance)
        }
    }

    private fun createOrganizationWithWallet(user: TestUser, organizationName: String) {
        suppose("User has wallet") {
            val walletWithBalance = BackendService.getUserWallet(user.token)
            assertNotNull(walletWithBalance)
        }
        suppose("Organization exists") {
            DatabaseUtil.insertOrganizationInDb(organizationName, user.userId)
            user.organizationId = DatabaseUtil.getOrganizationIdForName(organizationName)
                    ?: fail("Missing organization with name: $organizationName")
        }

        suppose("User is an admin of the organization") {
            DatabaseUtil.insertOrganizationMembershipInDb(user.userId, user.organizationId)
        }

        verify("User can create organization wallet") {
            val transactionToCreateOrganization = BackendService
                    .getTransactionToCreateOrganizationWallet(user.token, user.organizationId)
            val signedTransaction = BlockchainUtil
                    .signTransaction(transactionToCreateOrganization.transactionData, user.credentials)
            BackendService.createOrganizationWallet(user.token, user.organizationId, signedTransaction)
        }
        verify("User can get balance for organization wallet") {
            Thread.sleep(3000)
            val walletWithBalance = BackendService.getOrganizationWallet(user.token, user.organizationId)
            assertNotNull(walletWithBalance)
        }
    }

    private fun createProjectWithWallet(user: TestUser, projectName: String) {
        suppose("Project exists") {
            DatabaseUtil.insertProjectInDb(projectName, user.userId, user.organizationId)
            user.projectId = DatabaseUtil.getProjectIdForName(projectName)
                    ?: fail("Missing project with name: $projectName")
        }

        verify("User can create project wallet") {
            val transactionToCreateProject = BackendService
                    .getTransactionToCreateProjectWallet(user.token, user.projectId)
            val signedTransactionToCreateProject = BlockchainUtil
                    .signTransaction(transactionToCreateProject.transactionData, user.credentials)
            val projectWallet = BackendService
                    .createProjectWallet(user.token, user.projectId, signedTransactionToCreateProject)
            assertNotNull(projectWallet)
        }
    }

    private fun createUserWallet(token: String, credentials: Credentials): WalletResponse {
        val walletToken = BackendService.getWalletToken(token)
        val walletCreateRequest = WalletCreateRequest(
                credentials.address, BlockchainUtil.getPublicKey(credentials), walletToken.token
        )
        return BackendService.createUserWallet(token, walletCreateRequest)
    }

    private class TestUser(val email: String, val credentials: Credentials) {
        lateinit var token: String
        var userId = -1
        var organizationId = -1
        var projectId = -1
    }
}
