package com.nunchuk.android.wallet.confirm

sealed class WalletConfirmEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : WalletConfirmEvent()
    data class CreateWalletSuccessEvent(val descriptor: String) : WalletConfirmEvent()
    data class CreateWalletErrorEvent(val message: String) : WalletConfirmEvent()
}