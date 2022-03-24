package com.nunchuk.android.messages.components.list

import com.nunchuk.android.model.RoomWallet
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomsState(val rooms: List<RoomSummary>, val roomWallets: List<RoomWallet>) {

    companion object {
        fun empty() = RoomsState(emptyList(), emptyList())
    }

}

sealed class RoomsEvent {
    data class LoadingEvent(val loading: Boolean) : RoomsEvent()
}
