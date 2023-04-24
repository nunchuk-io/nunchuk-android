package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<UpdateCoinCollectionUseCase.Param, Boolean>(
    repository,
    nunchukNativeSdk,
    ioDispatcher
) {
    override suspend fun run(parameters: Param): Boolean {
        return nunchukNativeSdk.updateCoinCollection(
            walletId = parameters.walletId,
            coinCollection = parameters.coinCollection
        )
    }

    class Param(
        override val walletId: String,
        val coinCollection: CoinCollection,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}