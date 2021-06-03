package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface ValueFromAmountUseCase {
    suspend fun execute(amount: Amount): Result<String>
}

internal class ValueFromAmountUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ValueFromAmountUseCase {

    override suspend fun execute(amount: Amount) = exe {
        nativeSdk.valueFromAmount(amount)
    }

}