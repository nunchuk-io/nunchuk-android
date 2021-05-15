package com.nunchuk.android.nativelib

import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.*
import com.nunchuk.android.model.bridge.WalletBridge
import javax.inject.Inject
import javax.inject.Singleton

internal const val LIB_NAME = "nunchuk-android"

@Singleton
internal class LibNunchukAndroid @Inject constructor() {

    @Throws(NCNativeException::class)
    external fun initNunchuk(
        chain: Int,
        hwiPath: String,
        enableProxy: Boolean,
        testnetServers: List<String>,
        backendType: Int,
        storagePath: String
    )

    @Throws(NCNativeException::class)
    external fun createSigner(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): SingleSigner

    @Throws(NCNativeException::class)
    external fun createSoftwareSigner(
        name: String,
        mnemonic: String,
        passphrase: String = ""
    ): MasterSigner

    @Throws(NCNativeException::class)
    external fun getRemoteSigners(): List<SingleSigner>

    @Throws(NCNativeException::class)
    external fun createWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: Int,
        isEscrow: Boolean,
        description: String
    ): Wallet

    @Throws(NCNativeException::class)
    external fun getWallets(): List<Wallet>

    @Throws(NCNativeException::class)
    external fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String)

    @Throws(NCNativeException::class)
    external fun updateRemoteSigner(signer: SingleSigner)

    @Throws(NCNativeException::class)
    external fun draftWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: Int,
        isEscrow: Boolean,
        description: String
    ): String

    @Throws(NCNativeException::class)
    external fun exportWallet(
        walletId: String,
        filePath: String,
        format: Int
    ): Boolean

    @Throws(NCNativeException::class)
    external fun getWallet(walletId: String): Wallet

    @Throws(NCNativeException::class)
    external fun updateWallet(wallet: WalletBridge): Boolean

    @Throws(NCNativeException::class)
    external fun exportCoboWallet(walletId: String): List<String>

    @Throws(NCNativeException::class)
    external fun generateMnemonic(): String

    @Throws(NCNativeException::class)
    external fun getBip39WordList(): List<String>

    @Throws(NCNativeException::class)
    external fun checkMnemonic(mnemonic: String): Boolean

    @Throws(NCNativeException::class)
    external fun getMasterSigner(masterSignerId: String): MasterSigner

    @Throws(NCNativeException::class)
    external fun getMasterSigners(): List<MasterSigner>

    @Throws(NCNativeException::class)
    external fun getSignersFromMasterSigner(masterSignerId: String): List<SingleSigner>

    @Throws(NCNativeException::class)
    external fun deleteMasterSigner(masterSignerId: String): Boolean

    @Throws(NCNativeException::class)
    external fun updateMasterSigner(masterSigner: MasterSigner): Boolean

    @Throws(NCNativeException::class)
    external fun getUnusedSignerFromMasterSigner(
        masterSignerId: String,
        walletType: Int,
        addressType: Int
    ): SingleSigner

    @Throws(NCNativeException::class)
    external fun broadcastTransaction(walletId: String, txId: String): Transaction

    @Throws(NCNativeException::class)
    external fun createTransaction(
        walletId: String,
        outputs: Map<String, Amount>,
        memo: String,
        inputs: List<UnspentOutput>,
        feeRate: Amount,
        subtractFeeFromAmount: Boolean
    ): Transaction

    @Throws(NCNativeException::class)
    external fun draftTransaction(
        walletId: String,
        outputs: Map<String, Amount>,
        inputs: List<UnspentOutput>,
        feeRate: Amount,
        subtractFeeFromAmount: Boolean
    ): Transaction

    @Throws(NCNativeException::class)
    external fun deleteTransaction(walletId: String, txId: String): Boolean

    @Throws(NCNativeException::class)
    external fun exportCoboTransaction(walletId: String, txId: String): List<String>

    @Throws(NCNativeException::class)
    external fun exportTransaction(walletId: String, txId: String, filePath: String): Boolean

    @Throws(NCNativeException::class)
    external fun getTransaction(walletId: String, txId: String): Transaction

    @Throws(NCNativeException::class)
    external fun replaceTransaction(walletId: String, txId: String, newFeeRate: Amount): Transaction

    @Throws(NCNativeException::class)
    external fun importTransaction(walletId: String, filePath: String): Transaction

    @Throws(NCNativeException::class)
    external fun getTransactionHistory(walletId: String, count: Int, skip: Int): List<Transaction>

    @Throws(NCNativeException::class)
    external fun importCoboTransaction(walletId: String, qrData: List<String>): Transaction

    @Throws(NCNativeException::class)
    external fun updateTransactionMemo(walletId: String, txId: String, newMemo: String): Boolean

    @Throws(NCNativeException::class)
    external fun signTransaction(walletId: String, txId: String, device: Device)

    @Throws(NCNativeException::class)
    external fun exportTransactionHistory(walletId: String, filePath: String, format: Int)

    companion object {
        init {
            System.loadLibrary(LIB_NAME)
        }
    }
}
