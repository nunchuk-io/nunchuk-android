package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.HideBannerNewChatUseCase
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.usecase.message.CheckShowBannerNewChatUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.model.*
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.trySafe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.Timeline.Direction.BACKWARDS
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    accountManager: AccountManager,
    private val cancelWalletUseCase: CancelWalletUseCase,
    private val consumeEventUseCase: ConsumeEventUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase,
    private val createSharedWalletUseCase: CreateSharedWalletUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val hideBannerNewChatUseCase: HideBannerNewChatUseCase,
    private val checkShowBannerNewChatUseCase: CheckShowBannerNewChatUseCase,
    private val sendErrorEventUseCase: SendErrorEventUseCase,
    private val getDeveloperSettingUseCase: GetDeveloperSettingUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val sessionHolder: SessionHolder
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private var debugMode: Boolean = false

    private lateinit var room: Room

    private var timeline: Timeline? = null

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    private var latestPreviewableEventTs: Long = -1

    private var isConsumingEvents = AtomicBoolean(false)

    private val consumedEventIds = HashSet<String>()

    override val initialState = RoomDetailState.empty()

    private var handleTimeLineEventJob: Job? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()

    init {
        viewModelScope.launch {
            timelineListenerAdapter.data.debounce(500L).collect(::handleTimelineEvents)
        }
    }

    fun initialize(roomId: String) {
        sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
        getDeveloperSettings()
    }

    private fun onRetrievedRoom(room: Room) {
        markRoomDisplayed(room)
        storeRoom(room)
        joinRoom()
        initSendEventExecutor()
        retrieveTimelineEvents()
        getRoomWallet()
        checkInviteUser(room)
    }

    private fun checkInviteUser(room: Room) {
        getContactsUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                if (it.isNotEmpty() && (currentId in it.map(Contact::chatId))) {
                    leaveRoom(room)
                }
            }, {})
            .addToDisposables()
    }

    private fun leaveRoom(room: Room) {
        viewModelScope.launch {
            leaveRoomUseCase.execute(room.roomId)
                .flowOn(IO)
                .onException { }
                .collect { event(LeaveRoomEvent) }
        }
    }

    private fun markRoomDisplayed(room: Room) {
        viewModelScope.launch(IO) {
            trySafe { sessionHolder.getSafeActiveSession()?.roomService()?.onRoomDisplayed(room.roomId) }
        }
        viewModelScope.launch(IO) {
            trySafe { room.readService().markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT) }
        }
    }

    private fun getDeveloperSettings() {
        viewModelScope.launch {
            getDeveloperSettingUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect { debugMode = it.debugMode }
        }
    }

    private fun getRoomWallet(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId = room.roomId)
                .flowOn(IO)
                .onException {
                    handleUpdateMessagesContent(roomWallet = null)
                    sendErrorEvent(it)
                }
                .collect { roomWallet ->
                    onCompleted()
                    if (roomWallet != getState().roomWallet) {
                        handleUpdateMessagesContent(roomWallet = roomWallet)
                    }
                }
        }
    }

    private fun storeRoom(room: Room) {
        this.room = room
        sessionHolder.currentRoom = room
    }

    private fun initSendEventExecutor() {
        SendEventHelper.executor = object : SendEventExecutor {
            override fun execute(roomId: String, type: String, content: String, ignoreError: Boolean): String {
                Timber.d(" (${type}):  $content")
                if (sessionHolder.hasActiveSession()) {
                    sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)?.run {
                        trySafe { sendService().sendEvent(type, content.toMatrixContent()) }
                    }
                }
                return ""
            }
        }
    }

    private fun joinRoom() {
        viewModelScope.launch(IO) {
            trySafe { sessionHolder.getSafeActiveSession()?.roomService()?.joinRoom(room.roomId) }
        }
    }

    private fun retrieveTimelineEvents() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        timeline = room.timelineService().createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
            removeListener(timelineListenerAdapter)
            addListener(timelineListenerAdapter)
            start()
        }
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        Timber.tag(TAG).d("handleTimelineEvents:${events.size}")
        val displayableEvents =
            events.filter(TimelineEvent::isDisplayable).filterNot { !debugMode && it.isNunchukErrorEvent() }.groupEvents(loadMore = ::handleLoadMore)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent).filterNot(TimelineEvent::isNunchukErrorEvent)
            .sortedByDescending(TimelineEvent::time)
        handleTimeLineEventJob?.cancel()
        handleTimeLineEventJob = viewModelScope.launch {
            val consumableEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedByDescending(NunchukMatrixEvent::time)
            consumeEvents(consumableEvents, nunchukEvents)
        }
    }

    private suspend fun consumeEvents(
        sortedEvents: List<NunchukMatrixEvent>,
        nunchukEvents: List<TimelineEvent>
    ) {
        val unConsumeEvents = sortedEvents.filterNot { it.eventId in consumedEventIds }
        flowOf(unConsumeEvents)
            .map { events ->
                supervisorScope {
                    val tasks = events.map { event -> async { consumeEventUseCase(event) } }
                    tasks.awaitAll()
                }
            }
            .onStart { isConsumingEvents.set(true) }
            .flowOn(IO)
            .onException { sendErrorEvent(it) }
            .onCompletion {
                isConsumingEvents.set(false)
                consumedEventIds.addAll(unConsumeEvents.map { event -> event.eventId })
                handleUpdateMessagesContent()
                onConsumeEventCompleted(nunchukEvents)
                Timber.tag(TAG).d("onConsumeEventCompleted:${sortedEvents.size}")
            }
            .collect()
    }

    private fun onConsumeEventCompleted(nunchukEvents: List<TimelineEvent>) {
        val latestEventTs = room.roomSummary().latestPreviewableEventTs()
        if (latestEventTs != latestPreviewableEventTs) {
            latestPreviewableEventTs = latestEventTs
            event(HasUpdatedEvent)
            getRoomWallet(nunchukEvents)
        }
    }

    private fun getRoomWallet(nunchukEvents: List<TimelineEvent>) {
        getRoomWallet {
            getState().roomWallet?.walletId?.let {
                getTransactions(it, nunchukEvents.filter(TimelineEvent::isNunchukTransactionEvent))
                event(GetRoomWalletSuccessEvent)
            }
        }
    }

    private fun getTransactions(walletId: String, events: List<TimelineEvent>) {
        val eventFilterNotLocal = events.filterNot {
            it.eventId.startsWith("\$local.")
        }
        viewModelScope.launch {
            val eventIds = mapTransactionEvents(eventFilterNotLocal)
            getTransactionsUseCase.execute(walletId, eventIds)
                .flowOn(IO)
                .onException { sendErrorEvent(it) }
                .flowOn(Main)
                .collect { newTrans ->
                    if (newTrans != getState().transactions) {
                        handleUpdateMessagesContent(transactions = newTrans)
                        event(HasUpdatedEvent)
                    }
                }
        }
    }

    private fun mapTransactionEvents(events: List<TimelineEvent>) = events.filter { it.isInitTransactionEvent() || it.isReceiveTransactionEvent() }
        .map { it.eventId to it.isReceiveTransactionEvent() }

    fun handleSendMessage(content: String) {
        room.sendService().sendTextMessage(content)
    }

    fun handleTitleClick() {
        if (room.isDirectChat()) {
            event(OpenChatInfoEvent)
        } else {
            event(OpenChatGroupInfoEvent)
        }
    }

    fun handleLoadMore() {
        if (!isConsumingEvents.get() && timeline?.hasMoreToLoad(BACKWARDS).orFalse()) {
            isConsumingEvents.set(true)
            timeline?.paginate(BACKWARDS, PAGINATION)
        }
    }

    fun cancelWallet() {
        viewModelScope.launch {
            cancelWalletUseCase.execute(room.roomId)
                .flowOn(IO)
                .onException { sendErrorEvent(it) }
                .flowOn(Main)
                .collect { getRoomWallet() }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            createSharedWalletUseCase.execute(room.roomId)
                .flowOn(IO)
                .onException { sendErrorEvent(it) }
                .flowOn(Main)
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

    fun handleAddEvent() {
        val roomWallet = getState().roomWallet
        if (roomWallet == null) {
            event(CreateNewSharedWallet)
        } else {
            viewModelScope.launch {
                getWalletUseCase.execute(walletId = roomWallet.walletId)
                    .flowOn(IO)
                    .onException { sendErrorEvent(it) }
                    .flowOn(Main)
                    .collect { onGetWallet(it.wallet) }
            }
        }
    }

    fun handleReceiveEvent() {
        val roomWallet = getState().roomWallet
        if (roomWallet == null) {
            event(CreateNewSharedWallet)
        } else {
            event(ReceiveBTCEvent(roomWallet.walletId))
        }
    }

    private fun onGetWallet(wallet: Wallet) {
        if (wallet.balance.value > 0L) {
            event(CreateNewTransaction(roomId = room.roomId, walletId = wallet.id, availableAmount = wallet.balance.pureBTC()))
        }
    }

    fun hideBannerNewChat() {
        viewModelScope.launch {
            hideBannerNewChatUseCase.execute()
                .flowOn(IO)
                .catch { }
                .collect { event(HideBannerNewChatEvent) }
        }
    }

    fun checkShowBannerNewChat() {
        viewModelScope.launch {
            checkShowBannerNewChatUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect {
                    if (!it) {
                        event(HideBannerNewChatEvent)
                    }
                }
        }
    }

    private fun sendErrorEvent(t: Throwable) {
        sendErrorEvent(room.roomId, t, sendErrorEventUseCase::execute)
    }

    fun markMessageRead(eventId: String) {
        viewModelScope.launch(IO) {
            room.readService().setReadReceipt(eventId = eventId)
        }
    }

    private fun handleUpdateMessagesContent(
        roomWallet: RoomWallet? = getState().roomWallet,
        transactions: List<TransactionExtended> = getState().transactions,
        isSelectedEnable: Boolean = getState().isSelectEnable
    ) {
        viewModelScope.launch {
            val newMessages = withContext(IO) {
                val displayableEvents =
                    timelineListenerAdapter.getLastTimeEvents().filter(TimelineEvent::isDisplayable)
                        .filterNot { !debugMode && it.isNunchukErrorEvent() }
                        .groupEvents(loadMore = ::handleLoadMore)
                displayableEvents.toMessages(currentId, roomWallet, transactions, isSelectedEnable, getState().selectedEventIds)
            }
            updateState { copy(messages = newMessages, transactions = transactions, roomWallet = roomWallet, isSelectEnable = isSelectedEnable) }
        }
    }

    fun applySelected(isSelectedEnable: Boolean) {
        if (isSelectedEnable != getState().isSelectEnable) {
            handleUpdateMessagesContent(isSelectedEnable = isSelectedEnable)
        }
        if (isSelectedEnable.not()) {
            getState().selectedEventIds.clear()
        }
    }

    fun isSelectedEnable() = getState().isSelectEnable

    fun toggleSelected(eventId: Long) {
        if (getState().selectedEventIds.contains(eventId).not()) {
            getState().selectedEventIds.add(eventId)
        } else {
            getState().selectedEventIds.remove(eventId)
        }
        handleUpdateMessagesContent(isSelectedEnable = true)
    }

    override fun onCleared() {
        timeline?.apply {
            dispose()
            removeAllListeners()
        }
        sessionHolder.currentRoom = null
        consumedEventIds.clear()
        super.onCleared()
    }

    fun handleRoomTransactionCreated() {
        // TODO
//        timeline?.apply {
//            dispose()
//            removeAllListeners()
//            timelineListenerAdapter = TimelineListenerAdapter(::handleTimelineEvents)
//        }
//        updateState { RoomDetailState.empty() }
//        retrieveTimelineEvents()
    }

    companion object {
        private const val TAG = "RoomDetailViewModel"
    }

}
