package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    val repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<CreateCoinCollectionUseCase.Param, CoinCollection>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param): CoinCollection {
        val createCoinCollection = nunchukNativeSdk.createCoinCollection(
            walletId = parameters.walletId,
            name = parameters.coinCollection.name,
        )
        nunchukNativeSdk.updateCoinCollection(
            walletId = parameters.walletId,
            coinCollection = parameters.coinCollection.copy(id = createCoinCollection.id)
        )
        return createCoinCollection
    }

    class Param(
        override val walletId: String,
        val coinCollection: CoinCollection,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}