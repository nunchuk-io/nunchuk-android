package com.nunchuk.android.contact.components.pending.receive

import com.nunchuk.android.model.ReceiveContact

data class ReceivedState(val contacts: List<ReceiveContact> = emptyList())

sealed class ReceivedEvent {
    data class LoadingEvent(val loading: Boolean) : ReceivedEvent()
}