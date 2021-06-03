package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetMasterSignersUseCase {
    suspend fun execute(): Result<List<MasterSigner>>
}

internal class GetMasterSignersUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetMasterSignersUseCase {

    override suspend fun execute() = exe { nativeSdk.getMasterSigners() }

}