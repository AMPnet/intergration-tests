package com.ampnet.integration.tests.backend

data class WalletCreateRequest(
    val address: String,
    val publicKey: String
)
data class SignedTransaction(val data: String)
