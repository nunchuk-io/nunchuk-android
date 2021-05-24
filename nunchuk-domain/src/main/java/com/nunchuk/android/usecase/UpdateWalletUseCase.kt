package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface UpdateWalletUseCase {
    suspend fun execute(wallet: Wallet): Result<Boolean>
}

internal class UpdateWalletUseCaseImpl @Inject constructor(
    private val facade: LibNunchukFacade
) : BaseUseCase(), UpdateWalletUseCase {

    override suspend fun execute(wallet: Wallet) = exe {
        facade.updateWallet(wallet)
    }

}