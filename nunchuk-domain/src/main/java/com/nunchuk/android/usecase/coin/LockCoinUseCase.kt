package com.nunchuk.android.usecase.coin

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LockCoinUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<LockCoinUseCase.Params, Boolean>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Boolean {
        return nunchukNativeSdk.lockCoin(parameters.walletId, parameters.txId, parameters.vout)
    }

    data class Params(val walletId: String, val txId: String, val vout: Int,)
}