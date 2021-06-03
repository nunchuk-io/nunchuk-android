package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface CreateSignerUseCase {
    suspend fun execute(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): Result<SingleSigner>
}

internal class CreateSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CreateSignerUseCase {

    override suspend fun execute(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ) = exe {
        nativeSdk.createSigner(
            name = name,
            xpub = xpub,
            publicKey = publicKey,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint
        )
    }
}