package com.nunchuk.android.app.splash

import com.nunchuk.android.contact.components.pending.sent.SentEvent

sealed class GuestModeEvent {
    data class LoadingEvent(val loading: Boolean) : GuestModeEvent()
    data class InitErrorEvent(val error: String) : GuestModeEvent()
    object  InitSuccessEvent : GuestModeEvent()
}