package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<CreateCoinTagUseCase.Param, CoinTag>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param): CoinTag {
        return nunchukNativeSdk.createCoinTag(
            walletId = parameters.walletId,
            name = parameters.name,
            color = parameters.color
        )
    }

    class Param(
        override val walletId: String,
        val name: String,
        val color: String,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}