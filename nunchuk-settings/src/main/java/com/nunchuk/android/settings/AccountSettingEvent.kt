package com.nunchuk.android.settings

sealed class AccountSettingEvent {
    object Loading : AccountSettingEvent()
    object RequestDeleteSuccess : AccountSettingEvent()
    object DeletePrimaryKeySuccess : AccountSettingEvent()
    data class RequestDeleteError(val message: String) : AccountSettingEvent()
    data class CheckNeedPassphraseSent(val isNeeded: Boolean) : AccountSettingEvent()
}
