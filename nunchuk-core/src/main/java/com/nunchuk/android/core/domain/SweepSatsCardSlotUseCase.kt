package com.nunchuk.android.core.domain

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SweepSatsCardSlotUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<SweepSatsCardSlotUseCase.Data, Transaction>(dispatcher) {
    override suspend fun execute(parameters: Data) : Transaction {
        return nunchukNativeSdk.sweepBalance(parameters.slots, parameters.address, parameters.feeRate)
    }

    class Data(val address: String, val slots: List<SatsCardSlot>, val feeRate: Int)
}