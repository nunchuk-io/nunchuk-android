package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetMasterSignerUseCase {
    suspend fun execute(masterSignerId: String): Result<MasterSigner>
}

internal class GetMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetMasterSignerUseCase {

    override suspend fun execute(masterSignerId: String) = exe { nativeSdk.getMasterSigner(masterSignerId) }

}