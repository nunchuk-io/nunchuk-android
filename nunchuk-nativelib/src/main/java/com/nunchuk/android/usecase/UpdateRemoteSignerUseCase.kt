package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface UpdateRemoteSignerUseCase {
    suspend fun execute(signer: SingleSigner): Result<Boolean>
}

internal class UpdateRemoteSignerUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), UpdateRemoteSignerUseCase {

    override suspend fun execute(signer: SingleSigner) = exe {
        nunchukFacade.updateSigner(signer)
    }

}