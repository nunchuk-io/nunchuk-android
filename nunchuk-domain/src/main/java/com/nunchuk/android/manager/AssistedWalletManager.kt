package com.nunchuk.android.manager

import kotlinx.coroutines.flow.Flow

interface AssistedWalletManager {
    val assistedWalletIds : Flow<Set<String>>
    fun isAssistedWallet(walletId: String): Boolean
}