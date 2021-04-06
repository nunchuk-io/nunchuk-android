package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface DeleteRemoteSignerUseCase {
    suspend fun execute(masterFingerprint: String, derivationPath: String): Result<Unit>
}

internal class DeleteRemoteSignerUseCaseImpl @Inject constructor(
    val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), DeleteRemoteSignerUseCase {

    override suspend fun execute(masterFingerprint: String, derivationPath: String) = exe {
        nunchukFacade.deleteRemoteSigner(masterFingerprint = masterFingerprint, derivationPath = derivationPath)
    }
}