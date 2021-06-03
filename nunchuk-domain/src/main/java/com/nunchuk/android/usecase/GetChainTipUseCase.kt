package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface GetChainTipUseCase {
    suspend fun execute(): Result<Int>
}

internal class GetChainTipUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), GetChainTipUseCase {

    override suspend fun execute() = exe {
        nativeSdk.getChainTip()
    }

}