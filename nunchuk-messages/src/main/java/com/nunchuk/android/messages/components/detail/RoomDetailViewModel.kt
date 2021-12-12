package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CheckShowBannerNewChatUseCase
import com.nunchuk.android.core.domain.DontShowBannerNewChatUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.model.*
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
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
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val dontShowBannerNewChatUseCase: DontShowBannerNewChatUseCase,
    private val checkShowBannerNewChatUseCase: CheckShowBannerNewChatUseCase
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private lateinit var room: Room

    private lateinit var timeline: Timeline

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    private var timelineListenerAdapter = TimelineListenerAdapter(::handleTimelineEvents)

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

    private fun getRoomWallet(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId = room.roomId)
                .flowOn(IO)
                .onException {
                    updateState { copy(roomWallet = null) }
                }
                .collect {
                    onCompleted()
                    updateState { copy(roomWallet = it) }
                }
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
        timeline.removeListener(timelineListenerAdapter)
        timeline.addListener(timelineListenerAdapter)
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        val displayableEvents = events.filter(TimelineEvent::isDisplayable).groupEvents(loadMore = ::handleLoadMore)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedBy(NunchukMatrixEvent::time)
            consumeEvents(sortedEvents, displayableEvents, nunchukEvents)
        }
    }

    private suspend fun consumeEvents(
        sortedEvents: List<NunchukMatrixEvent>,
        displayableEvents: List<TimelineEvent>,
        nunchukEvents: List<TimelineEvent>
    ) {
        consumeEventUseCase.execute(sortedEvents)
            .flowOn(IO)
            .onException {}
            .flowOn(Main)
            .onCompletion {
                updateState { copy(messages = displayableEvents.toMessages(currentId)) }
                getRoomWallet(nunchukEvents)
            }
            .collect {
                Timber.d("Consume event completed")
            }
    }

    private fun getRoomWallet(nunchukEvents: List<TimelineEvent>) {
        getRoomWallet {
            getState().roomWallet?.walletId?.let {
                getTransactions(it, nunchukEvents.filter(TimelineEvent::isNunchukTransactionEvent))
            }
        }
    }

    private fun getTransactions(walletId: String, events: List<TimelineEvent>) {
        viewModelScope.launch {
            val eventIds = mapTransactionEvents(events)
            getTransactionsUseCase.execute(walletId, eventIds)
                .onException { }
                .collect { updateState { copy(transactions = it) } }
        }
    }

    private fun mapTransactionEvents(events: List<TimelineEvent>): List<Pair<String, Boolean>> {
        return events.filter { it.isInitTransactionEvent() || it.isReceiveTransactionEvent() }
            .map {
                it.eventId to it.isReceiveTransactionEvent()
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
                .onException {}
                .collect { getRoomWallet() }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            createSharedWalletUseCase.execute(room.roomId)
                .flowOn(IO)
                .onException { }
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
        timeline.removeListener(timelineListenerAdapter)
        timelineListenerAdapter = TimelineListenerAdapter {}
        SessionHolder.currentRoom = null
    }

    fun handleAddEvent() {
        val roomWallet = getState().roomWallet
        if (roomWallet == null) {
            event(CreateNewSharedWallet)
        } else {
            viewModelScope.launch {
                getWalletUseCase.execute(walletId = roomWallet.walletId)
                    .onException { }
                    .flowOn(Main)
                    .collect { onGetWallet(it.wallet) }
            }
        }

    }

    private fun onGetWallet(wallet: Wallet) {
        if (wallet.balance.value > 0L) {
            event(
                CreateNewTransaction(
                    roomId = room.roomId,
                    walletId = wallet.id,
                    availableAmount = wallet.balance.pureBTC()
                )
            )
        }
    }

    fun dontShowBannerNewChat() {
        viewModelScope.launch {
            dontShowBannerNewChatUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect {
                    event(DontShowBannerNewChatEvent)
                }
        }
    }

    fun checkShowBannerNewChat() {
        viewModelScope.launch {
            checkShowBannerNewChatUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect {
                    event(CheckShowBannerNewChatEvent(it))
                }
        }
    }

}

