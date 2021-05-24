package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
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
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CreateSignerUseCase {

    override suspend fun execute(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ) = exe {
        nunchukFacade.createSigner(
            name = name,
            xpub = xpub,
            publicKey = publicKey,
            derivationPath = derivationPath,
            masterFingerprint = masterFingerprint
        )
    }
}