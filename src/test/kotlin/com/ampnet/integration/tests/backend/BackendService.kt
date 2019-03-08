package com.ampnet.integration.tests.backend

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import java.io.File
import kotlin.test.fail

object BackendService {

    private const val backendUrl = "http://localhost:8123"

    private val mapper: ObjectMapper by lazy {
        val mapper = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        mapper
    }

    fun getHealthStatus(): Health? {
        val response = Fuel.get("$backendUrl/actuator/health").responseObject<Health>()
        return response.third.component1()
    }

    /* User */
    fun getJwtToken(email: String, password: String): String {
        val response = Fuel.post("$backendUrl/token")
                .jsonBody("""{
                    |"login_method": "EMAIL",
                    |"credentials": {
                    |       "email": "$email",
                    |       "password": "$password"
                    |   }
                    |}
                """.trimMargin())
                .responseObject<AuthTokenResponse>(mapper)
        return response.third.get().token
    }

    fun getUserWallet(token: String): WalletResponse? {
        val response = Fuel.get("$backendUrl/wallet")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode == 404) {
            return null
        }
        return response.third.get()
    }

    fun createUserWallet(token: String, request: WalletCreateRequest): WalletResponse {
        val response = Fuel.post("$backendUrl/wallet")
                .authentication()
                .bearer(token)
                .jsonBody(mapper.writeValueAsString(request))
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not create user wallet")
        return response.third.get()
    }

    /* Organization */
    fun getTransactionToCreateOrganizationWallet(token: String, organizationId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/wallet/organization/$organizationId/transaction")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get transaction to create organization wallet")
        return response.third.get()
    }

    fun createOrganizationWallet(token: String, organizationId: Int, signedTransaction: String): WalletResponse {
        val params = signedTransactionToParams(signedTransaction)
        val response = Fuel.post("$backendUrl/wallet/organization/$organizationId/transaction", params)
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not create organization wallet")
        return response.third.get()
    }

    fun getOrganizationWallet(token: String, organizationId: Int): WalletResponse {
        val response = Fuel.get("$backendUrl/wallet/organization/$organizationId")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get organization wallet")
        return response.third.get()
    }

    fun approveOrganization(token: String, organizationId: Int): OrganizationWithDocumentResponse {
        val response = Fuel.post("$backendUrl/organization/$organizationId/approve")
                .authentication()
                .bearer(token)
                .responseObject<OrganizationWithDocumentResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not approve organization")
        return response.third.get()
    }

    fun addDocument(token: String, organizationId: Int, fileLocation: String, fileName: String): DocumentResponse {
        val response = Fuel.upload("$backendUrl/organization/$organizationId/document")
                .add(FileDataPart(File(fileLocation), name = "file", filename=fileName))
                .authentication()
                .bearer(token)
                .responseObject<DocumentResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not add document to organization")
        return response.third.get()
    }

    /* Project */
    fun getTransactionToCreateProjectWallet(token: String, projectId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/wallet/project/$projectId/transaction")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get transaction to create project wallet")
        return response.third.get()
    }

    fun createProjectWallet(token: String, projectId: Int, signedTransaction: String): WalletResponse {
        val params = signedTransactionToParams(signedTransaction)
        val response = Fuel.post("$backendUrl/wallet/project/$projectId/transaction", params)
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not create project wallet")
        return response.third.get()
    }

    fun getProjectWallet(token: String, projectId: Int): WalletResponse {
        val response = Fuel.get("$backendUrl/wallet/project/$projectId")
                .authentication()
                .bearer(token)
                .responseObject<WalletResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get project wallet")
        return response.third.get()
    }

    fun getProjectInvestTransaction(token: String, projectId: Int, amount: Long): TransactionResponse {
        val params = listOf("amount" to amount)
        val response = Fuel.get("$backendUrl/project/$projectId/invest", params)
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get project invest transaction")
        return response.third.get()
    }

    fun investInProject(signedTransaction: String): TxHashResponse {
        val params = signedTransactionToParams(signedTransaction)
        val response = Fuel.post("$backendUrl/project/invest", params)
                .responseObject<TxHashResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not invest in project")
        return response.third.get()
    }

    fun getConfirmInvestmentTransaction(token: String, projectId: Int): TransactionResponse {
        val response = Fuel.get("$backendUrl/project/$projectId/invest/confirm")
                .authentication()
                .bearer(token)
                .responseObject<TransactionResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not get project confirm investment transaction")
        return response.third.get()
    }

    fun confirmInvestment(signedTransaction: String): TxHashResponse {
        val params = signedTransactionToParams(signedTransaction)
        val response = Fuel.post("$backendUrl/project/invest/confirm", params)
                .responseObject<TxHashResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not confirm investment in project")
        return response.third.get()
    }

    /* Issuing Authority */
    fun generateMintTransaction(from: String, userEmail: String, amount: Long): TransactionResponse {
        val params = listOf("from" to from, "email" to userEmail, "amount" to amount)
        val response = Fuel.get("$backendUrl/issuer/mint", params)
                .responseObject<TransactionResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not generate mint transaction")
        return response.third.get()
    }

    fun postAuthorityTransaction(signedTransaction: String, type: String): TxHashResponse {
        val request = SignedTransaction(signedTransaction)
        val response = Fuel.post("$backendUrl/issuer/transaction/$type")
                .jsonBody(mapper.writeValueAsString(request))
                .responseObject<TxHashResponse>(mapper)
        if (response.second.statusCode != 200) fail("Could not post mint transaction")
        return response.third.get()
    }

    private fun signedTransactionToParams(signedTransaction: String) = listOf("d" to signedTransaction)
}
