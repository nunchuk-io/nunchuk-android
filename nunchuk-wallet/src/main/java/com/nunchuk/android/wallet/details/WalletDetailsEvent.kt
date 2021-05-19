package com.nunchuk.android.wallet.details

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.Wallet

sealed class WalletDetailsEvent {
    data class WalletDetailsError(val message: String) : WalletDetailsEvent()
}

data class WalletDetailsState(
    val wallet: Wallet = Wallet(),
    val transactions: List<Transaction> = emptyList()
)