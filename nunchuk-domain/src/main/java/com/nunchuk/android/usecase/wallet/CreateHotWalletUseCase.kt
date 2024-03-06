package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateHotWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): Wallet {
        return nativeSdk.createHotWallet()
            ?: throw IllegalStateException("Failed to create hot wallet")
    }
}