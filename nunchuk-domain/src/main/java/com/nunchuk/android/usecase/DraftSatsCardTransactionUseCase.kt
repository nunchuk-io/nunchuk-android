package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DraftSatsCardTransactionUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<DraftSatsCardTransactionUseCase.Data, Transaction>(dispatcher) {
    override suspend fun execute(parameters: Data) : Transaction {
        return nunchukNativeSdk.draftSatscardTransaction(parameters.slots, parameters.address, parameters.feeRate)
    }

    class Data(val address: String, val slots: List<SatsCardSlot>, val feeRate: Int)
}