package com.nunchuk.android.messages.room.detail

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.room.detail.RoomDetailEvent.RoomNotFoundEvent
import com.nunchuk.android.messages.util.addMessageListener
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class RoomDetailViewModel @Inject constructor(
    private val accountManager: AccountManager
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private lateinit var room: Room

    override val initialState = RoomDetailState.empty()

    fun initialize(roomId: String) {
        SessionHolder.currentSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        updateState { copy(roomInfo = room.getRoomInfo()) }
        retrieveData()
    }

    private fun retrieveData() {
        room.addMessageListener {
            updateState { copy(messages = it.toMessages(accountManager.getAccount().chatId)) }
        }
    }

    fun handleSendMessage(content: String) {
        room.sendTextMessage(content)
    }

}

fun Room.getRoomInfo(): RoomInfo {
    val roomSummary = roomSummary()
    return if (roomSummary != null) {
        RoomInfo(roomSummary.displayName, roomSummary.joinedMembersCount ?: 0)
    } else {
        RoomInfo.empty()
    }

}

internal fun List<TimelineEvent>.toMessages(chatId: String) = sortedBy { it.root.ageLocalTs }.map { it.toMessage(chatId) }

fun TimelineEvent.toMessage(chatId: String) = Message(
    sender = senderInfo.displayName ?: "Guest",
    content = root.getClearContent().toModel<MessageContent>()?.body.orEmpty(),
    type = if (chatId == senderInfo.userId) MessageType.CHAT_MINE.index else MessageType.CHAT_PARTNER.index
)