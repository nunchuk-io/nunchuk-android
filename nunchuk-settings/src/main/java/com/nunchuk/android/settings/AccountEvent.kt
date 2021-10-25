package com.nunchuk.android.settings

import com.nunchuk.android.core.account.AccountInfo

data class AccountState(
    val appVersion: String = "",
    val account: AccountInfo = AccountInfo()
)

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
    data class GetUserProfileSuccessEvent(val name: String? = null, val avatarUrl: String? = null) : AccountEvent()
    data class UploadPhotoSuccessEvent(val matrixUri: String? = null) : AccountEvent()
}