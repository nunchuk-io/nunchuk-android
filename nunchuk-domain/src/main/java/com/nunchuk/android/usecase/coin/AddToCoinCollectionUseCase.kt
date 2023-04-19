package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddToCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<AddToCoinCollectionUseCase.Param, Unit>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param) {
        parameters.coins.forEach { coin ->
            parameters.collectionIds.forEach { collectionId ->
                nunchukNativeSdk.addToCoinCollection(
                    walletId = parameters.walletId,
                    txId = coin.txid,
                    collectionId = collectionId,
                    vout = coin.vout
                )
            }
        }
    }

    class Param(
        override val walletId: String,
        val collectionIds: List<Int>,
        val coins: List<UnspentOutput>,
        override val isAssistedWallet: Boolean,
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}