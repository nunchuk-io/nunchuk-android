package com.nunchuk.android.main.components.tabs.chat.messages

import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class MessagesState(val rooms: List<RoomSummary>) {

    companion object {
        fun empty() = MessagesState(emptyList())
    }

}

sealed class MessagesEvent
