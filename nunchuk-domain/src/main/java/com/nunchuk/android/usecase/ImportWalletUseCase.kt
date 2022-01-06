package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportWalletUseCase {
    fun execute(filePath: String, name: String, description: String): Flow<Wallet>
}

internal class ImportWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportWalletUseCase {

    override fun execute(filePath: String, name: String, description: String) = flow {
        emit(nativeSdk.importWallet(filePath, name, description))
    }

}
