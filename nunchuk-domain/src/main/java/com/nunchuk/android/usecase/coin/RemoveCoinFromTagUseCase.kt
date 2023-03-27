package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RemoveCoinFromTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<RemoveCoinFromTagUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        parameters.coins.forEach { output ->
            parameters.tagIds.forEach { tagId ->
                nunchukNativeSdk.removeFromCoinTag(
                    walletId = parameters.walletId,
                    txId = output.txid,
                    tagId = tagId,
                    vout = output.vout
                )
            }
        }
    }

    class Param(val walletId: String, val tagIds: List<Int>, val coins: List<UnspentOutput>)
}