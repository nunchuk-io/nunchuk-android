package com.nunchuk.android.wallet.confirm

sealed class WalletConfirmEvent {
    object CreateWalletSuccessEvent : WalletConfirmEvent()
    data class CreateWalletErrorEvent(val message: String) : WalletConfirmEvent()
}