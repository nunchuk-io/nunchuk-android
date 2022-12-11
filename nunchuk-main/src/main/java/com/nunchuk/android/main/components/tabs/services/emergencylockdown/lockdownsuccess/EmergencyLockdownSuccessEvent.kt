package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

sealed class EmergencyLockdownSuccessEvent {
    data class Loading(val isLoading: Boolean) : EmergencyLockdownSuccessEvent()
    object SignOut : EmergencyLockdownSuccessEvent()
}