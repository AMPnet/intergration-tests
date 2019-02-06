package com.ampnet.integration.tests.util

import com.ampnet.integration.tests.backend.TransactionData
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric

object BlockchainUtil {

    val alice = Credentials.create("0x16675095b2ebbe3402d71c018158a8cef7b8cdad650e716de17c487190133932")
    val bob = Credentials.create("0xb93de5fb1b8a74a2a1858b1e336185331a3e40a266ca3afb9b689f12ff0e8e8b")
    val jane = Credentials.create("0x368e184997dd02a05cba483a01f63852ee649afd02d8ae74fb9369c579a0631c")

    fun getPublicKey(account: Credentials): String =
            Numeric.toHexString(account.ecKeyPair.publicKey.toByteArray())

    fun signTransaction(transaction: TransactionData, credentials: Credentials): String {
        val rawTransaction = RawTransaction.createTransaction(
                transaction.nonce.toBigInteger(),
                transaction.gasPrice.toBigInteger(),
                transaction.gasLimit.toBigInteger(),
                transaction.to,
                transaction.data
        )
        return sign(rawTransaction, credentials)
    }

    private fun sign(rawTx: RawTransaction, credentials: Credentials): String {
        val signedTx = TransactionEncoder.signMessage(rawTx, credentials)
        return Numeric.toHexString(signedTx)
    }
}
