package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface DeleteMasterSignerUseCase {
    suspend fun execute(masterSignerId: String): Result<Boolean>
}

internal class DeleteMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DeleteMasterSignerUseCase {

    override suspend fun execute(masterSignerId: String) = exe {
        nativeSdk.deleteMasterSigner(masterSignerId)
    }

}