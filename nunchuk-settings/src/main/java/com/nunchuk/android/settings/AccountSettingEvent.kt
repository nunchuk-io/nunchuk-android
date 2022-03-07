package com.nunchuk.android.settings

sealed class AccountSettingEvent {
    object Loading : AccountSettingEvent()
    object RequestDeleteSuccess : AccountSettingEvent()
    data class RequestDeleteError(val message: String) : AccountSettingEvent()
}
