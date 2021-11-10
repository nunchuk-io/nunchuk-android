package com.nunchuk.android.wallet.components.review

sealed class ReviewWalletEvent {
    data class SetLoadingEvent(val showLoading: Boolean) : ReviewWalletEvent()
    data class CreateWalletSuccessEvent(val walletId: String) : ReviewWalletEvent()
    data class CreateWalletErrorEvent(val message: String) : ReviewWalletEvent()
}