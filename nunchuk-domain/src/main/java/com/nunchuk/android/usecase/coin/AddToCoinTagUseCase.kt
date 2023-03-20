package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddToCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<AddToCoinTagUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        parameters.coins.forEach { coin ->
            parameters.tagIds.forEach { tagId ->
                nunchukNativeSdk.addToCoinTag(
                    walletId = parameters.walletId,
                    txId = coin.txid,
                    tagId = tagId,
                    vout = coin.vout
                )
            }
        }
    }

    class Param(val walletId: String, val tagIds: List<Int>, val coins: List<UnspentOutput>)
}