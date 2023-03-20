package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<UpdateCoinTagUseCase.Param, Boolean>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Boolean {
        return nunchukNativeSdk.updateCoinTag(
            walletId = parameters.walletId,
            coinTag = parameters.coinTag
        )
    }

    class Param(val walletId: String, val coinTag: CoinTag)
}