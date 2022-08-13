package com.nunchuk.android.usecase

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface DraftTransactionUseCase {
    suspend fun execute(
        walletId: String,
        outputs: Map<String, Amount>,
        inputs: List<TxInput> = emptyList(),
        feeRate: Amount = Amount(-1),
        subtractFeeFromAmount: Boolean = false
    ): Result<Transaction>
}

internal class DraftTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DraftTransactionUseCase {
    override suspend fun execute(
        walletId: String,
        outputs: Map<String, Amount>,
        inputs: List<TxInput>,
        feeRate: Amount,
        subtractFeeFromAmount: Boolean
    ) = exe {
        nativeSdk.draftTransaction(
            walletId = walletId,
            outputs = outputs,
            inputs = inputs,
            feeRate = feeRate,
            subtractFeeFromAmount = subtractFeeFromAmount
        )
    }

}