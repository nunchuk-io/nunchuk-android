package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<UpdateCoinTagUseCase.Param, Boolean>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param): Boolean {
        return nunchukNativeSdk.updateCoinTag(
            walletId = parameters.walletId,
            coinTag = parameters.coinTag
        )
    }

    data class Param(
        override val walletId: String,
        val coinTag: CoinTag,
        override val isAssistedWallet: Boolean,
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}