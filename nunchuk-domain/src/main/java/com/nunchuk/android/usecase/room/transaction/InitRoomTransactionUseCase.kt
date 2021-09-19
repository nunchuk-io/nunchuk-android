package com.nunchuk.android.usecase.room.transaction

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface InitRoomTransactionUseCase {
    fun execute(
        roomId: String,
        outputs: Map<String, Amount>,
        memo: String = "",
        inputs: List<UnspentOutput> = emptyList(),
        feeRate: Amount = Amount(-1),
        subtractFeeFromAmount: Boolean = false
    ): Flow<NunchukMatrixEvent>
}

internal class InitRoomTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : InitRoomTransactionUseCase {

    override fun execute(
        roomId: String,
        outputs: Map<String, Amount>,
        memo: String,
        inputs: List<UnspentOutput>,
        feeRate: Amount,
        subtractFeeFromAmount: Boolean
    ) = flow {
        emit(
            nativeSdk.initRoomTransaction(
                roomId = roomId,
                outputs = outputs,
                memo = memo,
                inputs = inputs,
                feeRate = feeRate,
                subtractFeeFromAmount = subtractFeeFromAmount
            )
        )
    }

}