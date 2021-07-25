package com.nunchuk.android.messages.components.list

import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomsState(val rooms: List<RoomSummary>) {

    companion object {
        fun empty() = RoomsState(emptyList())
    }

}

sealed class RoomsEvent {
    data class LoadingEvent(val loading: Boolean) : RoomsEvent()
}
