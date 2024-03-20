package com.nunchuk.android.usecase.qr

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ExportBBQRWalletUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<ExportBBQRWalletUseCase.Params, List<String>>(dispatcher) {
    override suspend fun execute(parameters: Params): List<String> {
        val wallet = nunchukNativeSdk.getWallet(parameters.walletId)
        return nunchukNativeSdk.exportBBQRWallet(wallet, parameters.density)
    }

    data class Params(val walletId: String, val density: Int)
}