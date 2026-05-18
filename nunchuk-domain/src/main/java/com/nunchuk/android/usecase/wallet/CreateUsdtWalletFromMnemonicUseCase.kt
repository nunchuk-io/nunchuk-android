package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateUsdtWalletFromMnemonicUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreateUsdtWalletFromMnemonicUseCase.Param, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Wallet {
        return nativeSdk.createLiquidWallet(
            mnemonic = parameters.mnemonic,
            passphrase = parameters.passphrase,
        )
    }

    data class Param(
        val mnemonic: String = "",
        val passphrase: String = "",
    )
}
