package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface DeleteRemoteSignerUseCase {
    suspend fun execute(masterFingerprint: String, derivationPath: String): Result<Unit>
}

internal class DeleteRemoteSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DeleteRemoteSignerUseCase {

    override suspend fun execute(masterFingerprint: String, derivationPath: String) = exe {
        nativeSdk.deleteRemoteSigner(masterFingerprint = masterFingerprint, derivationPath = derivationPath)
    }
}