package com.nunchuk.android.nativelib

import com.nunchuk.android.model.SingleSigner
import javax.inject.Inject
import javax.inject.Singleton

internal const val LIB_NAME = "nunchuk-android"

@Singleton
internal class LibNunchukAndroid @Inject constructor() {

    external fun initNunchuk(
        chain: Int,
        hwiPath: String,
        enableProxy: Boolean,
        testnetServers: List<String>,
        backendType: Int,
        storagePath: String
    )

    external fun createSigner(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): SingleSigner

    external fun getRemoteSigner(): SingleSigner

    companion object {
        init {
            System.loadLibrary(LIB_NAME)
        }
    }
}
