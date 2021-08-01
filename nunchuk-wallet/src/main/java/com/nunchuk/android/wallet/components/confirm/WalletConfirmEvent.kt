package com.nunchuk.android.wallet.components.confirm

sealed class WalletConfirmEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : WalletConfirmEvent()
    data class CreateWalletSuccessEvent(val walletId: String, val descriptor: String) : WalletConfirmEvent()
    data class CreateWalletErrorEvent(val message: String) : WalletConfirmEvent()
}