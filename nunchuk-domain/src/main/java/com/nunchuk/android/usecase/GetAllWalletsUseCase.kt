package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAllWalletsUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val assistedWalletManager: AssistedWalletManager,
) : UseCase<Unit, List<WalletExtended>>(ioDispatcher) {
    override suspend fun execute(parameters: Unit): List<WalletExtended> {
        val wallets = nativeSdk.getWallets()
        val rWalletIds = nativeSdk.getAllRoomWalletIds()
        return wallets.map {
            val name = assistedWalletManager.getWalletAlias(it.id).ifEmpty { it.name }
            WalletExtended(it.copy(name = name), it.isShared(rWalletIds))
        }
    }

    private fun Wallet.isShared(rWalletIds: List<String>) = id in rWalletIds
}