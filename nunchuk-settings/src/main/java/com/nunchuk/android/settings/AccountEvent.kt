package com.nunchuk.android.settings

import com.nunchuk.android.core.account.AccountInfo

data class AccountState(
    val account: AccountInfo = AccountInfo(),
    val syncProgress: Int = 0,
    val finishedSync: Boolean = false
) {
    fun isSyncing() = syncProgress in 1..99
}

sealed class AccountEvent {
    object SignOutEvent : AccountEvent()
    data class GetUserProfileSuccessEvent(val name: String? = null, val avatarUrl: String? = null) : AccountEvent()
    object GetUserProfileGuestEvent : AccountEvent()
    data class UploadPhotoSuccessEvent(val matrixUri: String? = null) : AccountEvent()
    data class LoadingEvent(val loading: Boolean = false) : AccountEvent()
}