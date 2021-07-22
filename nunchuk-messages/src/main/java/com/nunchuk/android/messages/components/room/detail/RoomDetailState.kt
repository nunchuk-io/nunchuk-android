package com.nunchuk.android.messages.components.room.detail

data class RoomDetailState(val roomInfo: RoomInfo, val messages: List<Message>) {

    companion object {
        fun empty() = RoomDetailState(RoomInfo.empty(), emptyList())
    }

}

data class RoomInfo(val roomName: String, val memberCount: Int) {
    companion object {
        fun empty() = RoomInfo("", 0)
    }
}

sealed class RoomDetailEvent {
    object RoomNotFoundEvent : RoomDetailEvent()
    object ContactNotFoundEvent : RoomDetailEvent()
}