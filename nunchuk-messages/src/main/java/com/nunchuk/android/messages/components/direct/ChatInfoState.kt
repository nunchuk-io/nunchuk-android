package com.nunchuk.android.messages.components.direct

import com.nunchuk.android.model.Contact

data class ChatInfoState(val contact: Contact? = null)

sealed class ChatInfoEvent {
    object RoomNotFoundEvent : ChatInfoEvent()
}