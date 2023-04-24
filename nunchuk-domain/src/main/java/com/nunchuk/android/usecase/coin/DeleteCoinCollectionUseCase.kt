package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeleteCoinCollectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<DeleteCoinCollectionUseCase.Param, Unit>(repository, nunchukNativeSdk, ioDispatcher) {
    override suspend fun run(parameters: Param) {
        nunchukNativeSdk.deleteCoinCollection(
            walletId = parameters.walletId,
            collectionId = parameters.collectionId,
        )
    }

    class Param(
        override val walletId: String,
        val collectionId: Int,
        override val isAssistedWallet: Boolean
    ) : BaseSyncCoinUseCase.Param(walletId, isAssistedWallet)
}