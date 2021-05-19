package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface ValueFromAmountUseCase {
    suspend fun execute(amount: Amount): Result<String>
}

internal class ValueFromAmountUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), ValueFromAmountUseCase {

    override suspend fun execute(amount: Amount) = exe {
        nunchukFacade.valueFromAmount(amount)
    }

}