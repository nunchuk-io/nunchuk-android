package com.nunchuk.android.app.splash

sealed class GuestModeEvent {
    data class LoadingEvent(val loading: Boolean) : GuestModeEvent()
    data class InitErrorEvent(val error: String) : GuestModeEvent()
    object InitSuccessEvent : GuestModeEvent()
    object OpenSignInScreen : GuestModeEvent()
}