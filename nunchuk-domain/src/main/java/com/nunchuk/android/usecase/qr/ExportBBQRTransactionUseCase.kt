package com.nunchuk.android.usecase.qr

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ExportBBQRTransactionUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<ExportBBQRTransactionUseCase.Params, List<String>>(dispatcher) {
    override suspend fun execute(parameters: Params): List<String> {
        val tx = nunchukNativeSdk.getTransaction(walletId = parameters.walletId, txId = parameters.txId)
        return nunchukNativeSdk.exportBBQRTransaction(tx.psbt, parameters.density)
    }

    data class Params(val walletId: String, val txId: String, val density: Int)
}