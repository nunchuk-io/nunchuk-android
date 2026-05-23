package com.nunchuk.android.manager

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.WalletType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WalletManagerImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
) : WalletManager {
    private val walletTypes = ConcurrentHashMap<String, WalletType>()

    override fun getWalletType(walletId: String): WalletType =
        walletTypes.getOrPut(walletId) { nativeSdk.getWallet(walletId).walletType }
}
