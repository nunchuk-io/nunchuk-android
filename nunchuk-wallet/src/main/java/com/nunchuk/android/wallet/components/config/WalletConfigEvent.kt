package com.nunchuk.android.wallet.components.config

sealed class WalletConfigEvent {

    object UpdateNameSuccessEvent : WalletConfigEvent()

    data class UpdateNameErrorEvent(val message: String) : WalletConfigEvent()

    data class WalletDetailsError(val message: String) : WalletConfigEvent()

    object DeleteWalletSuccess : WalletConfigEvent()
}