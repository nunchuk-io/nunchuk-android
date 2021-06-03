package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface UpdateWalletUseCase {
    suspend fun execute(wallet: Wallet): Result<Boolean>
}

internal class UpdateWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), UpdateWalletUseCase {

    override suspend fun execute(wallet: Wallet) = exe {
        nativeSdk.updateWallet(wallet)
    }

}