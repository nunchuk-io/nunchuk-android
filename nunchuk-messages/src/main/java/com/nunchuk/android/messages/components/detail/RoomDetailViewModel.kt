package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.model.SendEventHelper
import com.nunchuk.android.usecase.ConsumeEventUseCase
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.Timeline.Direction.BACKWARDS
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import javax.inject.Inject

class RoomDetailViewModel @Inject constructor(
    accountManager: AccountManager,
    private val consumeEventUseCase: ConsumeEventUseCase,
    private val getAllRoomWalletsUseCase: GetAllRoomWalletsUseCase
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
        storeRoom(room)
        joinRoom()
        retrieveTimelineEvents()
        sendEvent()
        getAllRoomWallets()
    }

    private fun getAllRoomWallets() {
        getAllRoomWalletsUseCase.execute()
            .flowOn(IO)
            .catch { Timber.e(TAG, "get room wallets failed:$it") }
            .onEach { Timber.d(TAG, "room wallets $it") }
            .launchIn(viewModelScope)
    }

    private fun storeRoom(room: Room) {
        this.room = room
        SessionHolder.currentRoom = room
    }

    private fun sendEvent() {
        SendEventHelper.executor = object : SendEventExecutor {
            override fun execute(roomId: String, type: String, content: String): String {
                viewModelScope.launch {
                    room.sendEvent(type, content.toMatrixContent())
                }
                return ""
            }
        }
    }

    private fun joinRoom() {
        viewModelScope.launch {
            try {
                room.join()
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }

    fun retrieveTimelineEvents() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeAllListeners()
        timeline.addListener(TimelineListenerAdapter {
            Timber.d(TAG, "$it")
            val messages = it.filter(TimelineEvent::isDisplayable)
            updateState { copy(messages = messages.toMessages(currentId)) }
            consume(messages.filter(TimelineEvent::isNunchukEvent))
        })
        timeline.start()
    }

    private fun consume(events: List<TimelineEvent>) {
        viewModelScope.launch {
            events.asFlow()
                .flowOn(IO)
                .collect {
                    consume(it.toNunchukMatrixEvent())
                }
        }
    }

    private fun consume(event: NunchukMatrixEvent) {
        Timber.d(TAG, "consume($event)")
        consumeEventUseCase.execute(event)
            .flowOn(IO)
            .catch { Timber.e(TAG, "consume failed:$it") }
            .onEach { Timber.d(TAG, "consumed $event") }
            .launchIn(viewModelScope)
    }

    fun handleSendMessage(content: String) {
        room.sendTextMessage(content)
    }

    fun handleTitleClick() {
        if (room.isDirectChat()) {
            event(OpenChatInfoEvent)
        } else {
            event(OpenChatGroupInfoEvent)
        }
    }

    fun handleLoadMore() {
        if (timeline.hasMoreToLoad(BACKWARDS)) {
            timeline.paginate(BACKWARDS, PAGINATION)
        }
    }

    companion object {
        private const val TAG = "RoomDetailViewModel"
        private const val PAGINATION = 50
    }

}

