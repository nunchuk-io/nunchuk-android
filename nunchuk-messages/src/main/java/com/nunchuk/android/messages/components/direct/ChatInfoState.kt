package com.nunchuk.android.messages.components.direct

import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.RoomWallet

data class ChatInfoState(
    val contact: Contact? = null,
    val roomWallet: RoomWallet? = null
)

sealed class ChatInfoEvent {
    object RoomNotFoundEvent : ChatInfoEvent()
}