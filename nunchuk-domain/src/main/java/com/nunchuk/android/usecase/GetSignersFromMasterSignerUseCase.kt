package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetSignersFromMasterSignerUseCase {
    suspend fun execute(masterSignerId: String): Result<List<SingleSigner>>
}

internal class GetSignersFromMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetSignersFromMasterSignerUseCase {

    override suspend fun execute(masterSignerId: String) = exe { nativeSdk.getSignersFromMasterSigner(masterSignerId) }

}