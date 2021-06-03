package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

const val CONF_TARGET_PRIORITY = 2
const val CONF_TARGET_STANDARD = 6
const val CONF_TARGET_ECONOMICAL = 144

interface EstimateFeeUseCase {
    suspend fun execute(confTarget: Int = CONF_TARGET_STANDARD): Result<Amount>
}

internal class EstimateFeeUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), EstimateFeeUseCase {

    override suspend fun execute(confTarget: Int) = exe {
        nativeSdk.estimateFee(confTarget)
    }

}