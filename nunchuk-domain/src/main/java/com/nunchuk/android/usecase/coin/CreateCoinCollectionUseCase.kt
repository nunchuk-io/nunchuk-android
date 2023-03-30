package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<CreateCoinCollectionUseCase.Param, CoinCollection>(ioDispatcher) {
    override suspend fun execute(parameters: Param): CoinCollection {
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

    class Param(val walletId: String, val coinCollection: CoinCollection)
}