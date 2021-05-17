package com.nunchuk.android.wallet.details

import com.nunchuk.android.model.Wallet

sealed class WalletDetailsEvent {
    data class GetWalletDetailsError(val message: String) : WalletDetailsEvent()
}

data class WalletDetailsState(val wallet: Wallet = Wallet())