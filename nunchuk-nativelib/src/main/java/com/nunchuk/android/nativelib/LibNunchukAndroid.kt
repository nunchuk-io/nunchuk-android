package com.nunchuk.android.nativelib

import com.nunchuk.android.exception.NunchukNativeException
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import javax.inject.Inject
import javax.inject.Singleton

internal const val LIB_NAME = "nunchuk-android"

@Singleton
internal class LibNunchukAndroid @Inject constructor() {

    @Throws(NunchukNativeException::class)
    external fun initNunchuk(
        chain: Int,
        hwiPath: String,
        enableProxy: Boolean,
        testnetServers: List<String>,
        backendType: Int,
        storagePath: String
    )

    @Throws(NunchukNativeException::class)
    external fun createSigner(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): SingleSigner

    @Throws(NunchukNativeException::class)
    external fun getRemoteSigner(): SingleSigner

    @Throws(NunchukNativeException::class)
    external fun getRemoteSigners(): List<SingleSigner>

    @Throws(NunchukNativeException::class)
    external fun createWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: Int,
        isEscrow: Boolean,
        description: String
    ): Wallet

    @Throws(NunchukNativeException::class)
    external fun getWallets(): List<Wallet>

    @Throws(NunchukNativeException::class)
    external fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String)

    @Throws(NunchukNativeException::class)
    external fun updateSigner(signer: SingleSigner)

    @Throws(NunchukNativeException::class)
    external fun draftWallet(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: Int,
        isEscrow: Boolean,
        description: String
    ): String

    companion object {
        init {
            System.loadLibrary(LIB_NAME)
        }
    }
}
