package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.util.addMessageListener
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomDetailViewModel @Inject constructor(
    accountManager: AccountManager
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private lateinit var room: Room

    private val currentName = accountManager.getAccount().name
    private val currentId = accountManager.getAccount().chatId

    override val initialState = RoomDetailState.empty()

    fun initialize(roomId: String) {
        SessionHolder.currentSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        viewModelScope.launch {
            try {
                room.join()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        retrieveData()
    }

    private fun retrieveData() {
        room.addMessageListener {
            updateState { copy(messages = it.toMessages(currentId)) }
        }
    }

    fun handleSendMessage(content: String) {
        room.sendTextMessage(content)
    }

    fun handleTitleClick() {
        if (room.isDirectRoom()) {
            event(OpenChatInfoEvent)
        } else {
            event(OpenChatGroupInfoEvent)
        }
    }

}

fun Room.isDirectRoom(): Boolean {
    val queryParams = RoomMemberQueryParams.Builder().build()
    val roomMembers: List<RoomMemberSummary> = getRoomMembers(queryParams)
    return roomSummary()?.isDirect.orFalse() || roomMembers.size == 2
}

fun Room.getRoomInfo(currentName: String): RoomInfo {
    val roomSummary: RoomSummary? = roomSummary()
    return if (roomSummary != null) {
        RoomInfo(roomSummary.getRoomName(currentName), roomSummary.joinedMembersCount ?: 0)
    } else {
        RoomInfo.empty()
    }
}

fun Room.getRoomMemberList(): List<RoomMemberSummary> {
    val queryParams = RoomMemberQueryParams.Builder().build()
    return getRoomMembers(queryParams)
}

fun RoomSummary.getRoomName(currentName: String): String {
    val split = displayName.split(",")
    return if (split.size == 2) {
        split.firstOrNull { it != currentName }.orEmpty()
    } else {
        displayName
    }
}

internal fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.map { it.toMessage(chatId) }

fun TimelineEvent.toMessage(chatId: String) = Message(
    sender = senderInfo.displayName ?: "Guest",
    content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
    type = if (chatId == senderInfo.userId) MessageType.CHAT_MINE.index else MessageType.CHAT_PARTNER.index
)