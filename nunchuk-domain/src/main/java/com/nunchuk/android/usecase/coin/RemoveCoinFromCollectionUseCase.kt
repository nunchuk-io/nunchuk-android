package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RemoveCoinFromCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<RemoveCoinFromCollectionUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        parameters.coins.forEach { output ->
            parameters.collectionIds.forEach { collectionId ->
                nunchukNativeSdk.removeFromCoinCollection(
                    walletId = parameters.walletId,
                    txId = output.txid,
                    collectionId = collectionId,
                    vout = output.vout
                )
            }
        }
    }

    class Param(val walletId: String, val collectionIds: List<Int>, val coins: List<UnspentOutput>)
}