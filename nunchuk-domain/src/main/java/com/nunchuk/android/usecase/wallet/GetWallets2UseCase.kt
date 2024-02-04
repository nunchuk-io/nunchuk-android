package com.nunchuk.android.usecase.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetWallets2UseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val assistedWalletManager: AssistedWalletManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<Wallet>>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): List<Wallet> {
        return nativeSdk.getWallets().map {
            val name = assistedWalletManager.getWalletAlias(it.id).ifEmpty { it.name }
            it.copy(name = name)
        }
    }
}