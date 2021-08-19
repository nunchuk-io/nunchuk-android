package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.util.*
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.Timeline.Direction
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

class RoomDetailViewModel @Inject constructor(
    accountManager: AccountManager
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private lateinit var room: Room

    private lateinit var timeline: Timeline

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    override val initialState = RoomDetailState.empty()

    fun initialize(roomId: String) {
        SessionHolder.currentSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        this.room = room
        SessionHolder.currentRoom = room
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        viewModelScope.launch {
            try {
                room.join()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        retrieveData()
    }

    fun retrieveData() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        timeline.addListener(TimelineListenerAdapter {
            val messages = it.filter(TimelineEvent::isDisplayable)
            updateState { copy(messages = messages.toMessages(currentId)) }
        })
        timeline.start()
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

    fun handleLoadMore() {
        if (timeline.hasMoreToLoad(Direction.BACKWARDS)) {
            timeline.paginate(Direction.BACKWARDS, PAGINATION)
        }
    }

    companion object {
        private const val PAGINATION = 50
    }

}
