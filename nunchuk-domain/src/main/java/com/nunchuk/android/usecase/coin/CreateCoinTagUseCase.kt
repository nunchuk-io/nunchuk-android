package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<CreateCoinTagUseCase.Param, CoinTag>(ioDispatcher) {
    override suspend fun execute(parameters: Param): CoinTag {
        return nunchukNativeSdk.createCoinTag(
            walletId = parameters.walletId,
            name = parameters.name,
            color = parameters.color
        )
    }

    class Param(val walletId: String, val name: String, val color: String)
}