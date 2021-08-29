package com.nunchuk.android.usecase

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetWalletUseCase {
    fun execute(walletId: String): Flow<Wallet>
}

internal class GetWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetWalletUseCase {

    override fun execute(walletId: String) = flow {
        emit(nativeSdk.getWallet(walletId))
    }

}