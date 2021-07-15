package com.nunchuk.android.main.components.tabs.account

data class AccountState(val appVersion: String = "", val email: String = "")

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
}