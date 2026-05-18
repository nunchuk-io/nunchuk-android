package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignUsdtTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<SignUsdtTransactionUseCase.Param, Transaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Transaction {
        return nativeSdk.signLiquidTransaction(
            walletId = parameters.walletId,
            txId = parameters.txId,
            device = parameters.device,
        ).also {
            if (parameters.device.needPassPhraseSent && parameters.signerId.isNotEmpty()) {
                nativeSdk.clearSignerPassphrase(parameters.signerId)
            }
        }
    }

    data class Param(
        val walletId: String,
        val txId: String,
        val device: Device,
        val signerId: String,
    )
}
