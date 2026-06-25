package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class EstimateFeeForLiquidTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<EstimateFeeForLiquidTransactionUseCase.Param, Amount>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Amount {
        return nativeSdk.estimateFeeForLiquidTransaction(
            walletId = parameters.walletId,
            outputs = parameters.outputs,
            feeRate = parameters.feeRate,
            subtractFeeFromAmount = parameters.subtractFeeFromAmount,
        )
    }

    data class Param(
        val walletId: String,
        val outputs: Map<String, Map<String, Amount>>,
        val feeRate: Amount,
        val subtractFeeFromAmount: Boolean = false,
    )
}
