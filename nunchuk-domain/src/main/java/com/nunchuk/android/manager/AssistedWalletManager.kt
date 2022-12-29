package com.nunchuk.android.manager

import kotlinx.coroutines.flow.Flow

interface AssistedWalletManager {
    val assistedWalletId : Flow<String>
    fun isActiveAssistedWallet(walletId: String): Boolean
    fun isInactiveAssistedWallet(walletId: String): Boolean
}