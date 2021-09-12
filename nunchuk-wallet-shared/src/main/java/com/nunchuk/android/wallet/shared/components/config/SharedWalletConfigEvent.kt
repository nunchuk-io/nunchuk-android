package com.nunchuk.android.wallet.shared.components.config

sealed class SharedWalletConfigEvent {

    object UpdateNameSuccessEvent : SharedWalletConfigEvent()

    data class UpdateNameErrorEvent(val message: String) : SharedWalletConfigEvent()
}