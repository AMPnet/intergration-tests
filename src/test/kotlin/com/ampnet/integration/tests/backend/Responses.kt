package com.ampnet.integration.tests.backend

import java.time.ZonedDateTime
import java.util.UUID

data class Health(val status: String)
data class AccessRefreshTokenResponse(
        val accessToken: String,
        val expiresIn: Long,
        val refreshToken: String,
        val refreshTokenExpiresIn: Long
)

data class WalletResponse(
        val id: Int,
        val hash: String,
        val type: WalletType,
        val balance: Long,
        val currency: Currency,
        val createdAt: ZonedDateTime
)

data class TransactionResponse(
        val tx: TransactionData,
        val txId: Int,
        val info: TransactionInfoResponse
        // TODO: add infoSig
)

data class TransactionInfoResponse(
        val txType: TransactionType,
        val title: String,
        val description: String
)
data class TransactionAndLinkResponse(val tx: TransactionData, val link: String)
data class TransactionData(
        val data: String,
        val to: String,
        val nonce: Long,
        val gasLimit: Long,
        val gasPrice: Long,
        val value: Long,
        val publicKey: String
)
data class TxHashResponse(val txHash: String)

data class OrganizationWithDocumentResponse(
        val id: Int,
        val name: String,
        val createdAt: ZonedDateTime,
        val approved: Boolean,
        val legalInfo: String,
        val documents: List<DocumentResponse>
)
data class DocumentResponse(
        val id: Int,
        val link: String,
        val name: String,
        val type: String,
        val size: Int,
        val createdAt: ZonedDateTime
)

data class OrganizationMembershipsResponse(val members: List<OrganizationMembershipResponse>)
data class OrganizationMembershipResponse(
        val uuid: UUID,
        val firstName: String,
        val lastName: String,
        val role: String,
        val memberSince: ZonedDateTime
)

data class IdentyumToken(val token: String)

enum class Currency {
    EUR, HRK
}
enum class WalletType {
    ORG, USER, PROJECT
}
enum class TransactionType(val description: String) {
    CREATE_ORG("CreateOrgTx"),
    CREATE_PROJECT("CreateProjectTx"),
    INVEST_ALLOWANCE("InvestAllowanceTx"),
    INVEST("InvestTx")
}
