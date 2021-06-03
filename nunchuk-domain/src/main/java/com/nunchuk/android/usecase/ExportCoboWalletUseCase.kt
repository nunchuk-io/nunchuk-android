package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface ExportCoboWalletUseCase {
    suspend fun execute(walletId: String): Result<List<String>>
}

internal class ExportCoboWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ExportCoboWalletUseCase {

    override suspend fun execute(walletId: String) = exe {
        nativeSdk.exportCoboWallet(walletId)
    }
}