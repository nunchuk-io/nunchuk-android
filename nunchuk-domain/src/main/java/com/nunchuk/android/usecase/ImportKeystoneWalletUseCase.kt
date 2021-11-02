package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportKeystoneWalletUseCase {
    fun execute(qrData: List<String>, description: String): Flow<Wallet>
}

internal class ImportKeystoneWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportKeystoneWalletUseCase {

    override fun execute(qrData: List<String>, description: String) = flow {
        emit(nativeSdk.importKeystoneWallet(qrData = qrData, description = description))
    }

}
