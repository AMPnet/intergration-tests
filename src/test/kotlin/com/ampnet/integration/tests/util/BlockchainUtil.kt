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

    val alice: Credentials = Credentials.create("0xe8c7e2bdd82569189d73cb618391973c8f52d8ba651446a00a87cc67c8219781")
    val bob: Credentials = Credentials.create("0x215d6c308a70b5b74081ec71f7d67495b0c3c88bf0ba119b8eeb022cf2c20251")
//    val jane = Credentials.create("0xe57787b6142f659d759fcbe2ecda7e49f105fd61b10f73d980ab1964aef71132")

    val eurOwner: Credentials = Credentials.create("0xc0c85e6b373d090048676f0c82542cb3bb6793491450f478b147614262718cdd")

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
