package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DraftUsdtTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<DraftUsdtTransactionUseCase.Param, Transaction>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Transaction {
        return nativeSdk.draftLiquidTransaction(
            walletId = parameters.walletId,
            outputs = parameters.outputs,
            feeRate = parameters.feeRate,
        )
    }

    data class Param(
        val walletId: String,
        val outputs: Map<String, Map<String, Amount>>,
        val feeRate: Amount,
    )
}
