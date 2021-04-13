package com.nunchuk.android.main.components.tabs.account

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
}