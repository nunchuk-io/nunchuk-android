package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteCoinTagUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<DeleteCoinTagUseCase.Param, Unit>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param) {
        nunchukNativeSdk.deleteCoinTag(
            walletId = parameters.walletId,
            tagId = parameters.tagId,
        )
    }

    class Param(
        override val walletId: String,
        val tagId: Int,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}