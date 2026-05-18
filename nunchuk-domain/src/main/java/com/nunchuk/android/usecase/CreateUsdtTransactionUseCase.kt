package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateUsdtTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreateUsdtTransactionUseCase.Param, Transaction>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Transaction {
        return nativeSdk.createLiquidTransaction(
            walletId = parameters.walletId,
            outputs = parameters.outputs,
            feeRate = parameters.feeRate,
            memo = parameters.memo,
        )
    }

    data class Param(
        val walletId: String,
        val outputs: Map<String, Map<String, Amount>>,
        val feeRate: Amount,
        val memo: String = "",
    )
}
