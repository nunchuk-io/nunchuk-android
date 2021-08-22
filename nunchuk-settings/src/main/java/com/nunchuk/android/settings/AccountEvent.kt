package com.nunchuk.android.settings

import com.nunchuk.android.core.account.AccountInfo

data class AccountState(
    val appVersion: String = "",
    val account: AccountInfo = AccountInfo()
)

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
}