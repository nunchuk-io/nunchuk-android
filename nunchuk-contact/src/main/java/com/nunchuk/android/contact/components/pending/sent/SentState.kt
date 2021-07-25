package com.nunchuk.android.contact.components.pending.sent

import com.nunchuk.android.model.SentContact

data class SentState(val contacts: List<SentContact> = emptyList())

sealed class SentEvent {
    data class LoadingEvent(val loading: Boolean) : SentEvent()
}