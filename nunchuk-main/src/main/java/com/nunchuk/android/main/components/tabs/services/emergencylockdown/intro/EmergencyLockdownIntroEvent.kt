package com.nunchuk.android.main.components.tabs.services.emergencylockdown.intro

sealed class EmergencyLockdownIntroEvent {
    data class Loading(val isLoading: Boolean) : EmergencyLockdownIntroEvent()
    object ContinueClick : EmergencyLockdownIntroEvent()
}