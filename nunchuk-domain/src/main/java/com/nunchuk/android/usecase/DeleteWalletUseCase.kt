package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import javax.inject.Inject

interface DeleteWalletUseCase {
    suspend fun execute(walletId: String): Result<Boolean>
}

internal class DeleteWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DeleteWalletUseCase {

    override suspend fun execute(walletId: String) = exe {
        nativeSdk.deleteWallet(walletId)
    }

}