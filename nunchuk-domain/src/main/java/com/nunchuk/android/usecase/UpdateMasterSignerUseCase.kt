package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface UpdateMasterSignerUseCase {
    suspend fun execute(masterSigner: MasterSigner): Result<Boolean>
}

internal class UpdateMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), UpdateMasterSignerUseCase {
    override suspend fun execute(masterSigner: MasterSigner) = exe { nativeSdk.updateMasterSigner(masterSigner) }

}