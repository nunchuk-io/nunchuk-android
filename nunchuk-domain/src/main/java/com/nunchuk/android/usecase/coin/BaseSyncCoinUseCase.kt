package com.nunchuk.android.usecase.coin

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher

abstract class BaseSyncCoinUseCase<P : BaseSyncCoinUseCase.Param, R>(
    private val repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
    dispatcher: CoroutineDispatcher
) : UseCase<P, R>(dispatcher) {
    override suspend fun execute(parameters: P): R {
        return run(parameters).also {
             if (parameters.isAssistedWallet) {
                 val data = nunchukNativeSdk.exportCoinControlData(parameters.walletId)
                 if (data.isNotEmpty()) {
                     repository.uploadCoinControlData(parameters.walletId, data)
                 }
             }
        }
    }

    abstract suspend fun run(parameters: P) : R

    abstract class Param(open val walletId: String, open val isAssistedWallet: Boolean)
}