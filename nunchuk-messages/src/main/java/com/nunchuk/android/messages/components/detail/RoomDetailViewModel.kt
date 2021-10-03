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
import com.nunchuk.android.model.toRoomWalletData
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
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
    private val createSharedWalletUseCase: CreateSharedWalletUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
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
        initSendEventExecutor()
        retrieveTimelineEvents()
        getRoomWallet()
    }

    private fun getRoomWallet() {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId = room.roomId)
                .flowOn(IO)
                .catch {
                    updateState { copy(roomWallet = null) }
                    Timber.e("Get room wallet error ", it)
                }
                .collect { updateState { copy(roomWallet = it) } }
        }
    }

    private fun storeRoom(room: Room) {
        this.room = room
        SessionHolder.currentRoom = room
    }

    private fun initSendEventExecutor() {
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
                CrashlyticsReporter.recordException(e)
            }
        }
    }

    fun retrieveTimelineEvents() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeAllListeners()
        timeline.addListener(TimelineListenerAdapter(::handleTimelineEvents))
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        Timber.d("handleTimelineEvents")
        val displayableEvents = events.filter(TimelineEvent::isDisplayable)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedBy(NunchukMatrixEvent::time)
            consumeEventUseCase.execute(sortedEvents)
                .onCompletion {
                    updateState { copy(messages = displayableEvents.toMessages(currentId)) }
                    getTransactions(nunchukEvents.filter(TimelineEvent::isNunchukTransactionEvent))
                }
                .catch { CrashlyticsReporter.recordException(it) }
                .collect { Timber.d("Consume event completed") }
        }
    }

    private fun getTransactions(events: List<TimelineEvent>) {
        viewModelScope.launch {
            val eventIds = events.filter(TimelineEvent::isInitTransactionEvent).map(TimelineEvent::eventId)
            getTransactionsUseCase.execute(eventIds)
                .catch { CrashlyticsReporter.recordException(it) }
                .collect { updateState { copy(transactions = it) } }
        }
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
                .catch { CrashlyticsReporter.recordException(it) }
                .collect { getRoomWallet() }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            createSharedWalletUseCase.execute(room.roomId)
                .flowOn(IO)
                .catch { CrashlyticsReporter.recordException(it) }
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
        SessionHolder.currentRoom = null
    }

    companion object {
        private const val PAGINATION = 50
    }

}

