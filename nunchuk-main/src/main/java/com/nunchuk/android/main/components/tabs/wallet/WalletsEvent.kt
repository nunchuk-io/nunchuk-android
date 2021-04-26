package com.nunchuk.android.main.components.tabs.wallet

internal sealed class WalletsEvent {
    object AddWalletEvent : WalletsEvent()
    data class ShowErrorEvent(val message: String) : WalletsEvent()
    object ShowSignerIntroEvent : WalletsEvent()
}