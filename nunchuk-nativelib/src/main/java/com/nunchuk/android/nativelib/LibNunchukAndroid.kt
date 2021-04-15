package com.nunchuk.android.nativelib

import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.ExportFormat
import java.lang.Exception
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
    external fun getRemoteSigner(): SingleSigner

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
    external fun updateSigner(signer: SingleSigner)

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

    companion object {
        init {
            System.loadLibrary(LIB_NAME)
        }
    }
}
