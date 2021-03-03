package com.nunchuk.android.nativelib

import javax.inject.Inject

internal class LibNunchukFacade @Inject constructor(
        private val nunchukAndroid: LibNunchukAndroid
) {

    fun createSigner(
            name: String,
            xpub: String,
            publicKey: String,
            derivationPath: String,
            masterFingerprint: String
    ) = nunchukAndroid.createSigner(
            name = name,
            xpub = xpub,
            publicKey = publicKey,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint
    )

}