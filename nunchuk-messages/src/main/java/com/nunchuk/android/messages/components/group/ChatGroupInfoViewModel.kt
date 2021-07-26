package com.nunchuk.android.messages.components.group

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.getRoomMemberList
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

class ChatGroupInfoViewModel @Inject constructor(
) : NunchukViewModel<ChatGroupInfoState, ChatGroupInfoEvent>() {

    private lateinit var room: Room

    override val initialState = ChatGroupInfoState()

    fun initialize(roomId: String) {
        SessionHolder.currentSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(ChatGroupInfoEvent.RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        room.roomSummary()?.let {
            updateState { copy(summary = it) }
        }
        updateState { copy(roomMembers = room.getRoomMemberList()) }
    }
}