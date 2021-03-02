package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner

interface CreateSignerUseCase {
    fun execute(
            name: String,
            xpub: String,
            publicKey: String,
            derivationPath: String,
            masterFingerprint: String
    ): SingleSigner
}