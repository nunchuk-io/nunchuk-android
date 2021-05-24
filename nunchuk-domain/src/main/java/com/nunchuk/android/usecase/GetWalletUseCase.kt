package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.LibNunchukFacade
import javax.inject.Inject

interface GetWalletUseCase {
    suspend fun execute(walletId: String): Result<Wallet>
}

internal class GetWalletUseCaseImpl @Inject constructor(
    private val facade: LibNunchukFacade
) : BaseUseCase(), GetWalletUseCase {

    override suspend fun execute(walletId: String) = exe {
        facade.getWallet(walletId)
    }

}