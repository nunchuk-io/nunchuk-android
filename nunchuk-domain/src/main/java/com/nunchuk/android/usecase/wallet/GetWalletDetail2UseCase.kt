package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetWalletDetail2UseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val assistedWalletManager: AssistedWalletManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<String, Wallet>(ioDispatcher) {
    override suspend fun execute(parameters: String): Wallet {
        val wallet = nativeSdk.getWallet(parameters)
        val name = assistedWalletManager.getWalletAlias(parameters).ifEmpty { wallet.name }
        return wallet.copy(name = name)
    }
}