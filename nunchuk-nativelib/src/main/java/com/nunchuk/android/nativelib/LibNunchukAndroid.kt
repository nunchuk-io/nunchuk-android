package com.nunchuk.android.nativelib

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

internal const val LIB_NAME = "nunchuk-android"

@Singleton
internal class LibNunchukAndroid @Inject constructor() {

    @Throws(Exception::class)
    external fun initNunchuk(
        chain: Int,
        hwiPath: String,
        enableProxy: Boolean,
        testnetServers: List<String>,
        backendType: Int,
        storagePath: String
    )

    @Throws(Exception::class)
    external fun createSigner(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): SingleSigner

    @Throws(Exception::class)
    external fun getRemoteSigner(): SingleSigner

    @Throws(Exception::class)
    external fun getRemoteSigners(result: ArrayList<SingleSigner>): List<SingleSigner>

    @Throws(Exception::class)
    external fun getWallets(result: ArrayList<Wallet>): List<Wallet>

    @Throws(Exception::class)
    external fun deleteRemoteSigner(masterFingerprint: String, derivationPath: String)

    @Throws(Exception::class)
    external fun updateSigner(signer: SingleSigner)

    companion object {
        init {
            System.loadLibrary(LIB_NAME)
        }
    }
}
