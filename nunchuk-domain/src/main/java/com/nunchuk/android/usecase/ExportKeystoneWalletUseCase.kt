package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ExportKeystoneWalletUseCase {
    fun execute(walletId: String): Flow<List<String>>
}

internal class ExportKeystoneWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ExportKeystoneWalletUseCase {

    override fun execute(walletId: String) = flow {
        emit(nativeSdk.exportKeystoneWallet(walletId))
    }
}