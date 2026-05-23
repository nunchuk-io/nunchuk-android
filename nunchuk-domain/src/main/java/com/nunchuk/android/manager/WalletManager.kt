package com.nunchuk.android.manager

import com.nunchuk.android.type.WalletType

interface WalletManager {
    fun getWalletType(walletId: String): WalletType
}
