package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class EstimateRollOverTransactionAndAmountUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<EstimateRollOverTransactionAndAmountUseCase.Params, EstimateRollOverTransactionAndAmountUseCase.Result>(
    ioDispatcher
) {

    override suspend fun execute(parameters: Params): Result {
        val numOfTx = nativeSdk.estimateRollOverTransactionCount(
            walletId = parameters.oldWalletId,
            tags = parameters.tags, collections = parameters.collections
        )
        val (subAmount, fee) = nativeSdk.estimateRollOverAmount(
            walletId = parameters.oldWalletId,
            newWalletId = parameters.newWalletId,
            tags = parameters.tags,
            collections = parameters.collections,
            feeRate = parameters.feeRate
        )
        return Result(numOfTx, subAmount, fee)
    }

    class Params(
        val oldWalletId: String,
        val newWalletId: String,
        val tags: List<CoinTag>,
        val collections: List<CoinCollection>,
        val feeRate: Amount
    )

    class Result(
        val numOfTxs: Int, val subAmount: Amount, val feeAmount: Amount
    )
}