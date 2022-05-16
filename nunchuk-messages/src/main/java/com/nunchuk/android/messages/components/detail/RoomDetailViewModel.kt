package com.nunchuk.android.messages.components.detail

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.HideBannerNewChatUseCase
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.usecase.message.CheckShowBannerNewChatUseCase
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.model.*
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.trySafe
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
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
    private val hideBannerNewChatUseCase: HideBannerNewChatUseCase,
    private val checkShowBannerNewChatUseCase: CheckShowBannerNewChatUseCase,
    private val sendErrorEventUseCase: SendErrorEventUseCase,
    private val getDeveloperSettingUseCase: GetDeveloperSettingUseCase
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {

    private var debugMode: Boolean = false

    private var prepareToEncrypt: Boolean = true

    private lateinit var room: Room

    private lateinit var timeline: Timeline

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    private val currentEmail = accountManager.getAccount().email

    private var latestPreviewableEventTs: Long = -1

    private var timelineListenerAdapter = TimelineListenerAdapter(::handleTimelineEvents)

    override val initialState = RoomDetailState.empty()

    fun initialize(roomId: String) {
        SessionHolder.activeSession?.getRoom(roomId)?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
        getDeveloperSettings()
    }

    private fun onRetrievedRoom(room: Room) {
        markRoomDisplayed(room)
        storeRoom(room)
        joinRoom()
        initSendEventExecutor()
        retrieveTimelineEvents()
        getRoomWallet()
    }

    private fun markRoomDisplayed(room: Room) {
        viewModelScope.launch(IO) {
            trySafe { SessionHolder.activeSession?.onRoomDisplayed(room.roomId) }
        }
        viewModelScope.launch(IO) {
            trySafe { room.markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT) }
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
                    updateState { copy(roomWallet = null) }
                    sendErrorEvent(it)
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
            override fun execute(roomId: String, type: String, content: String, ignoreError: Boolean): String {
                if (SessionHolder.hasActiveSession()) {
                    SessionHolder.activeSession?.getRoom(roomId)?.run {
                        sendEvent(type, content.toMatrixContent())
                    }
                }
                return ""
            }
        }
    }

    private fun joinRoom() {
        viewModelScope.launch(IO) {
            if (EmailValidator.isNunchukEmail(currentEmail)) {
                trySafe { SessionHolder.activeSession?.joinRoom(room.roomId) }
            }
        }
    }

    private fun retrieveTimelineEvents() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        timeline = room.createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeListener(timelineListenerAdapter)
        timeline.addListener(timelineListenerAdapter)
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        val displayableEvents = events.filter(TimelineEvent::isDisplayable).filterNot { !debugMode && it.isNunchukErrorEvent() }.groupEvents(loadMore = ::handleLoadMore)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent).filterNot(TimelineEvent::isNunchukErrorEvent)
        viewModelScope.launch {
            val consumableEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedBy(NunchukMatrixEvent::time)
            consumeEvents(consumableEvents, displayableEvents, nunchukEvents)
        }
    }

    private fun consumeEvents(
        sortedEvents: List<NunchukMatrixEvent>,
        displayableEvents: List<TimelineEvent>,
        nunchukEvents: List<TimelineEvent>
    ) {
        viewModelScope.launch {
            consumeEventUseCase.execute(sortedEvents)
                .flowOn(IO)
                .onException { sendErrorEvent(it) }
                .onCompletion { onConsumeEventCompleted(displayableEvents, nunchukEvents) }
                .collect { Timber.d("Consume event completed") }
        }
    }

    private fun onConsumeEventCompleted(displayableEvents: List<TimelineEvent>, nunchukEvents: List<TimelineEvent>) {
        updateState { copy(messages = displayableEvents.toMessages(currentId)) }
        getRoomWallet(nunchukEvents)
        val latestEventTs = room.roomSummary().latestPreviewableEventTs()
        if (latestEventTs != latestPreviewableEventTs) {
            latestPreviewableEventTs = latestEventTs
            event(HasUpdatedEvent)
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
        viewModelScope.launch {
            val eventIds = mapTransactionEvents(events)
            getTransactionsUseCase.execute(walletId, eventIds)
                .flowOn(IO)
                .onException { sendErrorEvent(it) }
                .collect { updateState { copy(transactions = it) } }
        }
    }

    private fun mapTransactionEvents(events: List<TimelineEvent>) = events.filter { it.isInitTransactionEvent() || it.isReceiveTransactionEvent() }
        .map { it.eventId to it.isReceiveTransactionEvent() }

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
            room.setReadReceipt(eventId = eventId)
        }
    }

    override fun onCleared() {
        if (room.isEncrypted()) {
            prepareForEncryption()
        }
        timeline.dispose()
        timeline.removeAllListeners()
        super.onCleared()
    }

    private fun prepareForEncryption() {
        if (prepareToEncrypt) {
            viewModelScope.launch(IO) {
                runCatching { room.prepareToEncrypt() }.fold({ prepareToEncrypt = false }, { prepareToEncrypt = false })
            }
        }
    }

}

