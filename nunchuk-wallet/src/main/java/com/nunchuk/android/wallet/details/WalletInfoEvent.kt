package com.nunchuk.android.wallet.details

sealed class WalletInfoEvent {

    object UpdateNameSuccessEvent : WalletInfoEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletInfoEvent()
}