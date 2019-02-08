package com.ampnet.integration.tests.util

import com.ampnet.integration.tests.backend.TransactionData
import io.github.novacrypto.base58.Base58
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
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
        val signedTransaction = sign(rawTransaction, credentials)
        return convertToVaultEncoding(signedTransaction)
    }

    private fun sign(rawTx: RawTransaction, credentials: Credentials): String {
        val signedTx = TransactionEncoder.signMessage(rawTx, credentials)
        return Numeric.toHexString(signedTx)
    }

    private fun convertToVaultEncoding(signedRawTx: String): String {
        val version = RlpString.create("1")
        val type = RlpString.create("1")
        val protocol = RlpString.create("eth")
        val payload = RlpList(
                RlpString.create(signedRawTx),
                RlpString.create("account-identifier")
        )
        val rlpEncodedTx = RlpEncoder.encode(RlpList(version, type, protocol, payload))
        val fakeChecksum = byteArrayOf(0, 0, 0, 0)
        return Base58.base58Encode(rlpEncodedTx + fakeChecksum)
    }
}
