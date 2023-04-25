package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCoinTagAdditionListUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<String, List<CoinTagAddition>>(ioDispatcher) {
    override suspend fun execute(parameters: String): List<CoinTagAddition> {
        val coinTagAdditions = arrayListOf<CoinTagAddition>()
        val coinTags = nunchukNativeSdk.getCoinTags(parameters)
        coinTags.forEach {
            val unspentOutputs = nunchukNativeSdk.getCoinByTag(
                walletId = parameters,
                tagId = it.id
            )
            coinTagAdditions.add(CoinTagAddition(coinTag = it, numCoins = unspentOutputs.size))

        }
        return coinTagAdditions
    }
}