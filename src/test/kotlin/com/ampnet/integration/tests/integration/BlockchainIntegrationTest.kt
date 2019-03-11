package com.ampnet.integration.tests.integration

import com.ampnet.integration.tests.BaseTest
import com.ampnet.integration.tests.backend.BackendService
import com.ampnet.integration.tests.util.BlockchainUtil
import com.ampnet.integration.tests.util.DatabaseUtil
import com.ampnet.integration.tests.backend.WalletCreateRequest
import com.ampnet.integration.tests.backend.WalletResponse
import org.web3j.crypto.Credentials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class BlockchainIntegrationTest: BaseTest() {

    private val alice = TestUser("alice@email.com", BlockchainUtil.alice)
    private val bob = TestUser("bob@email.com", BlockchainUtil.bob)

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

    @Test
    fun investInProject() {
        suppose("Alice project exists") {
            createProject()
        }
        suppose("Bob has a wallet") {
            createUserWithWallet(bob)
        }
        suppose("Bob has some greenars on wallet") {
            val transaction = BackendService.generateMintTransaction(BlockchainUtil.eurOwner.address, bob.email, 10000)
            val signedTransaction = BlockchainUtil.signTransaction(transaction.transactionData, BlockchainUtil.eurOwner)
            val txHash = BackendService.postAuthorityTransaction(signedTransaction, "mint")
            assertNotNull(txHash)
        }

        verify("Bob can start investment in Alice project") {
            val transactionToInvest = BackendService.getProjectInvestTransaction(bob.token, alice.projectId, 1000)
            val signedTransaction = BlockchainUtil.signTransaction(transactionToInvest.transactionData, bob.credentials)
            val txHash = BackendService.investInProject(signedTransaction)
            assertNotNull(txHash)
        }
        verify("Bob can confirm investment in Alice project") {
            val transactionToConfirmInvestment = BackendService.getConfirmInvestmentTransaction(bob.token, alice.projectId)
            val signedTransaction = BlockchainUtil.signTransaction(transactionToConfirmInvestment.transactionData, bob.credentials)
            val txHash = BackendService.confirmInvestment(signedTransaction)
            assertNotNull(txHash)
        }
        // TODO: Fix this!
//        verify("Project did receive funds") {
//            val projectWalletWithBalance = BackendService.getProjectWallet(alice.token, alice.projectId)
//            assertEquals(1000, projectWalletWithBalance.balance)
//        }
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
        val walletCreateRequest = WalletCreateRequest(credentials.address, BlockchainUtil.getPublicKey(credentials))
        return BackendService.createUserWallet(token, walletCreateRequest)
    }

    private class TestUser(val email: String, val credentials: Credentials) {
        lateinit var token: String
        var userId = -1
        var organizationId = -1
        var projectId = -1
    }
}
