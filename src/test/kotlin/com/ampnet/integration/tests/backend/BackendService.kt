package com.ampnet.integration.tests.backend

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import java.io.File
import kotlin.test.fail

object BackendService {

    private const val backendUrl = "http://localhost:8123"

    fun getHealthStatus(): Health? {
        val response = Fuel.get("$backendUrl/actuator/health").responseObject<Health>()
        return response.third.component1()
    }

    /* User */
    fun getUserWallet(token: String): WalletResponse? {
        val response = Fuel.get("$backendUrl/wallet")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(JsonMapper.mapper)
        if (response.second.statusCode == 404) {
            return null
        }
        return response.third.get()
    }

    fun createUserWallet(token: String, request: WalletCreateRequest): WalletResponse {
        val response = Fuel.post("$backendUrl/wallet")
                .authentication()
                .bearer(token)
                .jsonBody(JsonMapper.mapper.writeValueAsString(request))
                .responseObject<WalletResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not create user wallet")
        return response.third.get()
    }

    /* Organization */
    fun getTransactionToCreateOrganizationWallet(token: String, organizationId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/wallet/organization/$organizationId/transaction")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get transaction to create organization wallet")
        return response.third.get()
    }

    fun getOrganizationWallet(token: String, organizationId: Int): WalletResponse {
        val response = Fuel.get("$backendUrl/wallet/organization/$organizationId")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get organization wallet")
        return response.third.get()
    }

    fun approveOrganization(token: String, organizationId: Int): OrganizationWithDocumentResponse {
        val response = Fuel.post("$backendUrl/organization/$organizationId/approve")
                .authentication()
                .bearer(token)
                .responseObject<OrganizationWithDocumentResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not approve organization")
        return response.third.get()
    }

    fun addDocument(token: String, organizationId: Int, fileLocation: String, fileName: String): DocumentResponse {
        val response = Fuel.upload("$backendUrl/organization/$organizationId/document")
                .add(FileDataPart(File(fileLocation), name = "file", filename=fileName))
                .authentication()
                .bearer(token)
                .responseObject<DocumentResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not add document to organization")
        return response.third.get()
    }

    fun removeDocument(token: String, organizationId: Int, documentId: Int) {
        val response = Fuel.delete("$backendUrl/organization/$organizationId/document/$documentId")
                .authentication()
                .bearer(token)
                .response()
        if (response.second.statusCode != 200) fail("Could not delete organization document")
    }

    /* Project */
    fun getTransactionToCreateProjectWallet(token: String, projectId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/wallet/project/$projectId/transaction")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get transaction to create project wallet")
        return response.third.get()
    }

    fun getProjectWallet(token: String, projectId: Int): WalletResponse {
        val response = Fuel.get("$backendUrl/wallet/project/$projectId")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get project wallet")
        return response.third.get()
    }

    fun getProjectInvestTransaction(token: String, projectId: Int, amount: Long): TransactionResponse {
        val params = listOf("amount" to amount)
        val response = Fuel.get("$backendUrl/project/$projectId/invest", params)
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get project invest transaction")
        return response.third.get()
    }

    fun getConfirmInvestmentTransaction(token: String, projectId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/project/$projectId/invest/confirm")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not get project confirm investment transaction")
        return response.third.get()
    }

    fun broadcastTransaction(signedTransaction: String, txId: Int): TxHashResponse {
        val params = listOf("tx_id" to txId, "tx_sig" to signedTransaction)
        val response = Fuel.post("$backendUrl/tx_broadcast", params)
                .responseObject<TxHashResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not broadcast transaction")
        return response.third.get()
    }

    /* Issuing Authority */
    fun generateMintTransaction(from: String, userUuid: String, amount: Long): TransactionAndLinkResponse {
        val params = listOf("from" to from, "uuid" to userUuid, "amount" to amount)
        val response = Fuel.get("$backendUrl/issuer/mint", params)
                .responseObject<TransactionAndLinkResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not generate mint transaction")
        return response.third.get()
    }

    fun postAuthorityTransaction(signedTransaction: String, type: String): TxHashResponse {
        val request = SignedTransaction(signedTransaction)
        val response = Fuel.post("$backendUrl/issuer/transaction/$type")
                .jsonBody(JsonMapper.mapper.writeValueAsString(request))
                .responseObject<TxHashResponse>(JsonMapper.mapper)
        if (response.second.statusCode != 200) fail("Could not post mint transaction")
        return response.third.get()
    }
}
