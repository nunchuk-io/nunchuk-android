package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RecoverHotWalletUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<RecoverHotWalletUseCase.Params, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Wallet {
        return nativeSdk.recoverHotWallet(parameters.mnemonic, parameters.replace)
            ?: throw IllegalStateException("Failed to create hot wallet")
    }

    class Params(val mnemonic: String, val replace: Boolean = false)
}