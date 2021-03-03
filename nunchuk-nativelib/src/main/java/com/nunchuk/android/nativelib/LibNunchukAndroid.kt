package com.nunchuk.android.nativelib

import com.nunchuk.android.model.SingleSigner
import javax.inject.Inject

class LibNunchukAndroid @Inject constructor() {

    external fun createSigner(
            name: String,
            xpub: String,
            publicKey: String,
            derivationPath: String,
            masterFingerprint: String
    ): SingleSigner

    external fun getFirstRemoteSigner(): SingleSigner

    companion object {
        init {
            System.loadLibrary("nunchuk-android")
        }
    }

}