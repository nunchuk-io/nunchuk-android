package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UnLockCoinUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : BaseSyncCoinUseCase<UnLockCoinUseCase.Params, Boolean>(
    repository,
    nunchukNativeSdk,
    ioDispatcher
) {

    override suspend fun run(parameters: Params): Boolean {
        return nunchukNativeSdk.unlockCoin(parameters.walletId, parameters.txId, parameters.vout)
    }

    data class Params(
        override val walletId: String,
        val txId: String,
        val vout: Int,
        override val isAssistedWallet: Boolean,
    ) : Param(walletId, isAssistedWallet)
}