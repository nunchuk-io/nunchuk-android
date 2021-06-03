package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface UpdateRemoteSignerUseCase {
    suspend fun execute(signer: SingleSigner): Result<Unit>
}

internal class UpdateRemoteSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), UpdateRemoteSignerUseCase {

    override suspend fun execute(signer: SingleSigner) = exe {
        nativeSdk.updateRemoteSigner(signer)
    }

}