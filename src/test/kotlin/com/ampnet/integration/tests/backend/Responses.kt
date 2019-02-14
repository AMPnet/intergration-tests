package com.ampnet.integration.tests.backend

import java.time.ZonedDateTime

data class Health(val status: String)
data class AuthTokenResponse(val token: String)
data class WalletResponse(
        val id: Int,
        val hash: String,
        val type: WalletType,
        val balance: Long,
        val currency: Currency,
        val createdAt: ZonedDateTime
)
data class WalletTokenResponse(
        val token: String,
        val createdAt: ZonedDateTime
)
data class TransactionResponse(
        val transactionData: TransactionData,
        val link: String
)
data class TransactionData(
        val data: String,
        val to: String,
        val nonce: Long,
        val gasLimit: Long,
        val gasPrice: Long,
        val value: Long,
        val publicKey: String
)
data class OrganizationWithDocumentResponse(
        val id: Int,
        val name: String,
        val createdByUser: String,
        val createdAt: ZonedDateTime,
        val approved: Boolean,
        val legalInfo: String,
        val documents: List<DocumentResponse>
)
data class DocumentResponse(
        val id: Int,
        val hash: String,
        val name: String,
        val type: String,
        val size: Int,
        val createdAt: ZonedDateTime
)
data class TxHashResponse(val txHash: String)

enum class Currency {
    EUR, HRK
}
enum class WalletType {
    ORG, USER, PROJECT
}
