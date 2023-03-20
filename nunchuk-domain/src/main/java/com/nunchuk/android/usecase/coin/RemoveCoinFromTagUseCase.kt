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
        parameters.coins.forEach {
            nunchukNativeSdk.removeFromCoinTag(
                walletId = parameters.walletId,
                txId = it.txid,
                tagId = parameters.tagId,
                vout = it.vout
            )
        }
    }

    class Param(val walletId: String, val tagId: Int, val coins: List<UnspentOutput>)
}