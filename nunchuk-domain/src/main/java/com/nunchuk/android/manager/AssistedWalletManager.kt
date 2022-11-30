package com.nunchuk.android.manager

import kotlinx.coroutines.flow.Flow

interface AssistedWalletManager {
    val assistedWalletId : Flow<String>
    fun isAssistedWallet(walletId: String): Boolean
}