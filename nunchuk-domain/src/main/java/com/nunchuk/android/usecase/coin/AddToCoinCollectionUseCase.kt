package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddToCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<AddToCoinCollectionUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
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

    class Param(val walletId: String, val collectionIds: List<Int>, val coins: List<UnspentOutput>)
}