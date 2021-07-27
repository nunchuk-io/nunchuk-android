package com.nunchuk.android.messages.components.group

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.messages.components.group.ChatGroupInfoEvent.*
import com.nunchuk.android.messages.util.getRoomMemberList
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.Failure
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

    fun handleEditName(name: String) {
        viewModelScope.launch {
            try {
                room.updateName(name)
                event(UpdateRoomNameSuccess(name))
            } catch (e: Throwable) {
                event(UpdateRoomNameError(e.toMatrixError()))
            }
        }
    }

    fun handleLeaveGroup() {
        viewModelScope.launch {
            try {
                room.leave()
                event(LeaveRoomSuccess)
            } catch (e: Throwable) {
                event(LeaveRoomError(e.toMatrixError()))
            }
        }
    }
}

fun Throwable.toMatrixError() = if (this is Failure.ServerError) {
    error.message
} else {
    message.orUnknownError()
}