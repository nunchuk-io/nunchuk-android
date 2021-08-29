package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface UpdateWalletUseCase {
    fun execute(wallet: Wallet): Flow<Boolean>
}

internal class UpdateWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : UpdateWalletUseCase {

    override fun execute(wallet: Wallet) = flow {
        emit(nativeSdk.updateWallet(wallet))
    }

}