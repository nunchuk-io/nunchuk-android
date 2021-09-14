package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.model.*
import com.nunchuk.android.usecase.CancelWalletUseCase
import com.nunchuk.android.usecase.ConsumeEventUseCase
import com.nunchuk.android.usecase.CreateSharedWalletUseCase
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
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
    private val cancelWalletUseCase: CancelWalletUseCase,
    private val consumeEventUseCase: ConsumeEventUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase,
    private val createSharedWalletUseCase: CreateSharedWalletUseCase
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private lateinit var room: Room

    private lateinit var timeline: Timeline

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    override val initialState = RoomDetailState.empty()

    fun initialize(roomId: String) {
        SessionHolder.activeSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
    }

    private fun onRetrievedRoom(room: Room) {
        storeRoom(room)
        joinRoom()
        getRoomWallet()
        retrieveTimelineEvents()
        sendEvent()
    }

    private fun getRoomWallet() {
        getRoomWalletUseCase.execute(roomId = room.roomId)
            .catch { Timber.e("get room failed:$it") }
            .onEach { onGetRoomWallet(it) }
            .launchIn(viewModelScope)
    }

    private fun onGetRoomWallet(roomWallet: RoomWallet) {
        updateState { copy(roomWallet = roomWallet) }
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
            val messages = it.filter(TimelineEvent::isDisplayable)
            updateState { copy(messages = messages.toMessages(currentId)) }
            consume(messages.filter(TimelineEvent::isNunchukEvent))
        })
        timeline.start()
    }

    private fun consume(events: List<TimelineEvent>) {
        viewModelScope.launch {
            events.map(TimelineEvent::toNunchukMatrixEvent)
                .sortedBy(NunchukMatrixEvent::time)
                .asFlow()
                .flowOn(IO)
                .collect { consume(it) }
        }
    }

    private fun consume(event: NunchukMatrixEvent) {
        consumeEventUseCase.execute(event)
            .flowOn(IO)
            .catch { Timber.e("\nconsume failed:$it") }
            .onEach {
                delay(500)
            }
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

    fun cancelWallet() {
        viewModelScope.launch {
            cancelWalletUseCase.execute(room.roomId)
                .catch { Timber.e("cancel wallet error", it) }
                .collect { getRoomWallet() }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            createSharedWalletUseCase.execute(room.roomId)
                .catch { Timber.e("finalize wallet error", it) }
                .collect {
                    getRoomWallet()
                    event(RoomWalletCreatedEvent)
                }
        }
    }

    fun viewConfig() {
        getState().roomWallet?.jsonContent?.toRoomWalletData()?.let {
            event(ViewWalletConfigEvent(room.roomId, it))
        }
    }

    fun denyWallet() {
        // FIXME
        cancelWallet()
    }

    fun cleanUp() {
        timeline.removeAllListeners()
    }

    companion object {
        private const val PAGINATION = 50
    }

}

