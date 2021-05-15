package com.nunchuk.android.wallet.config

sealed class WalletConfigEvent {

    object UpdateNameSuccessEvent : WalletConfigEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletConfigEvent()
}