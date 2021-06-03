package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetUnspentOutputsUseCase {
    suspend fun execute(walletId: String): Result<List<UnspentOutput>>
}

internal class GetUnspentOutputsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetUnspentOutputsUseCase {

    override suspend fun execute(walletId: String) = exe {
        nativeSdk.getUnspentOutputs(walletId)
    }

}