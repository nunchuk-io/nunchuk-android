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
        val isEmptyTagsAndCollections = parameters.tags.isEmpty() && parameters.collections.isEmpty()
        
        val numOfTx = if (isEmptyTagsAndCollections) {
            nativeSdk.estimateRollOver11TransactionCount(
                walletId = parameters.oldWalletId
            )
        } else {
            nativeSdk.estimateRollOverTransactionCount(
                walletId = parameters.oldWalletId,
                tags = parameters.tags,
                collections = parameters.collections
            )
        }
        
        val (subAmount, fee) = if (isEmptyTagsAndCollections) {
            nativeSdk.estimateRollOver11Amount(
                walletId = parameters.oldWalletId,
                newWalletId = parameters.newWalletId,
                feeRate = parameters.feeRate
            )
        } else {
            nativeSdk.estimateRollOverAmount(
                walletId = parameters.oldWalletId,
                newWalletId = parameters.newWalletId,
                tags = parameters.tags,
                collections = parameters.collections,
                feeRate = parameters.feeRate
            )
        }
        
        return Result(numOfTx, subAmount, fee)
    }

    class Params(
        val oldWalletId: String,
        val newWalletId: String,
        val tags: List<CoinTag> = emptyList(),
        val collections: List<CoinCollection> = emptyList(),
        val feeRate: Amount
    )

    class Result(
        val numOfTxs: Int, val subAmount: Amount, val feeAmount: Amount
    )
}