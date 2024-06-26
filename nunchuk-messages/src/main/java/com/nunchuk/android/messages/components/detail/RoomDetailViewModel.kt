/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.messages.components.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.HideBannerNewChatUseCase
import com.nunchuk.android.core.domain.SendErrorEventUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.GROUP_CHAT_ROOM_TYPE
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.ROOM_RETENTION
import com.nunchuk.android.core.util.SUPPORT_ROOM_USER_ID
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.isCreated
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toMatrixContent
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.CreateNewSharedWallet
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.CreateNewTransaction
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.GetRoomWalletSuccessEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.HasUpdatedEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.HideBannerNewChatEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.LeaveRoomEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.Loading
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.None
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.OnSendMediaSuccess
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.OpenChatGroupInfoEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.OpenChatInfoEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.OpenFile
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.ReceiveBTCEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.RoomNotFoundEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.RoomWalletCreatedEvent
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.ShowError
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.ViewWalletConfigEvent
import com.nunchuk.android.messages.usecase.media.SendMediaUseCase
import com.nunchuk.android.messages.usecase.message.CheckShowBannerNewChatUseCase
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.messages.util.downloadFile
import com.nunchuk.android.messages.util.getRoomInfo
import com.nunchuk.android.messages.util.getRoomMemberList
import com.nunchuk.android.messages.util.groupEvents
import com.nunchuk.android.messages.util.isDirectChat
import com.nunchuk.android.messages.util.isDisplayable
import com.nunchuk.android.messages.util.isInitTransactionEvent
import com.nunchuk.android.messages.util.isLocalEvent
import com.nunchuk.android.messages.util.isNunchukErrorEvent
import com.nunchuk.android.messages.util.isNunchukEvent
import com.nunchuk.android.messages.util.isReceiveTransactionEvent
import com.nunchuk.android.messages.util.latestPreviewableEventTs
import com.nunchuk.android.messages.util.time
import com.nunchuk.android.messages.util.toMessages
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SendEventExecutor
import com.nunchuk.android.model.SendEventHelper
import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.toRoomWalletData
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.usecase.CancelWalletUseCase
import com.nunchuk.android.usecase.ConsumeEventUseCase
import com.nunchuk.android.usecase.CreateSharedWalletUseCase
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import com.nunchuk.android.usecase.GetTransactionsUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.coin.IsMyCoinUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.trySafe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.Timeline.Direction.BACKWARDS
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

// TODO cache transaction with event
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
    private val sessionHolder: SessionHolder,
    private val sendMediaUseCase: SendMediaUseCase,
    private val isMyCoinUseCase: IsMyCoinUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : NunchukViewModel<RoomDetailState, RoomDetailEvent>() {
    private val args = RoomDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private var debugMode: Boolean = false

    private lateinit var room: Room

    private var timeline: Timeline? = null

    private val currentName = accountManager.getAccount().name

    private val currentId = accountManager.getAccount().chatId

    private var latestPreviewableEventTs: Long = -1

    private var isConsumingEvents = AtomicBoolean(false)

    private val consumedEventIds = HashSet<String>()

    override val initialState = RoomDetailState.empty()

    private var loadMessageJob: Job? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()

    private var hideMessageJob: Job? = null

    init {
        viewModelScope.launch {
            timelineListenerAdapter.data.filter { it.isNotEmpty() }.collect(::handleTimelineEvents)
        }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    result.onSuccess { groups ->
                        updateState { copy(isHasByzantineGroup = groups.isNotEmpty()) }
                    }
                }
        }
        viewModelScope.launch {
            runCatching {
                val room = sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(args.roomId)
                room?.stateService()?.getStateEventLive(ROOM_RETENTION, QueryStringValue.IsEmpty)
                    ?.asFlow()
                    ?.distinctUntilChanged()
                    ?.collect {
                        scheduleHideMessages()
                    }
            }
        }
        initialize()
    }

    private fun scheduleHideMessages() {
        hideMessageJob?.cancel()
        val stateEvent = room.stateService().getStateEvent("m.room.retention", QueryStringValue.IsEmpty)
        val maxLifetime = stateEvent?.content?.get("max_lifetime")?.toString()?.toDoubleOrNull()?.toLong()
        if (maxLifetime != null) {
            hideMessageJob = viewModelScope.launch {
                while (isActive) {
                    handleUpdateMessagesContent()
                    delay(1.minutes)
                }
            }
        }
    }

    private fun initialize() {
        val room = sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(args.roomId)
        room?.let(::onRetrievedRoom) ?: event(RoomNotFoundEvent)
        room?.timelineService()?.getAttachmentMessages()
        if (args.isGroupChat) updateState { copy(isGroupChatRoom = true) }
        else {
            room?.let {
                val stateEvent = room.stateService()
                    .getStateEvent(GROUP_CHAT_ROOM_TYPE, QueryStringValue.IsEmpty)
                updateState { copy(isGroupChatRoom = stateEvent != null) }
            }
        }
        getDeveloperSettings()
    }

    private fun onRetrievedRoom(room: Room) {
        updateState {
            copy(isSupportRoom = room.getRoomMemberList().any { it.userId == SUPPORT_ROOM_USER_ID })
        }
        markRoomDisplayed(room)
        storeRoom(room)
        joinRoom()
        initSendEventExecutor()
        retrieveTimelineEvents()
        getRoomWallet()
        checkInviteUser(room)
    }

    private fun checkInviteUser(room: Room) {
        viewModelScope.launch {
            getContactsUseCase.execute().catch { }.collect {
                if (it.isNotEmpty() && (currentId in it.map(Contact::chatId))) {
                    leaveRoom(room)
                }
            }
        }
    }

    private fun leaveRoom(room: Room) {
        viewModelScope.launch {
            leaveRoomUseCase.execute(room.roomId).flowOn(ioDispatcher).onException { }
                .collect { event(LeaveRoomEvent) }
        }
    }

    private fun markRoomDisplayed(room: Room) {
        viewModelScope.launch(ioDispatcher) {
            trySafe {
                sessionHolder.getSafeActiveSession()?.roomService()?.onRoomDisplayed(room.roomId)
            }
        }
        viewModelScope.launch(ioDispatcher) {
            trySafe { room.readService().markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT) }
        }
    }

    private fun getDeveloperSettings() {
        viewModelScope.launch {
            getDeveloperSettingUseCase.execute().flowOn(ioDispatcher).onException { }
                .collect {
                    debugMode = it.debugMode
                    updateState { copy(matrixBasedCollaborativeWalletEnabled = it.matrixBasedCollaborativeWallet) }
                }
        }
    }

    private fun getRoomWallet() {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId = room.roomId).flowOn(ioDispatcher).onException {
                handleUpdateMessagesContent(roomWallet = null)
                sendErrorEvent(it)
            }.collect { roomWallet ->
                if (roomWallet != getState().roomWallet) {
                    getTransactions()
                    handleUpdateMessagesContent(roomWallet = roomWallet)
                }
            }
        }
    }

    private fun storeRoom(room: Room) {
        this.room = room
        sessionHolder.setActiveRoom(room.roomId, false)
    }

    private fun initSendEventExecutor() {
        SendEventHelper.executor = object : SendEventExecutor {
            override fun execute(
                roomId: String, type: String, content: String, ignoreError: Boolean
            ): String {
                Timber.d(" (${type}):  $content")
                trySafe {
                    if (sessionHolder.hasActiveSession()) {
                        sessionHolder.getSafeActiveSession()?.roomService()?.getRoom(roomId)?.run {
                            sendService().sendEvent(type, content.toMatrixContent())
                        }
                    }
                }
                return ""
            }
        }
    }

    private fun joinRoom() {
        viewModelScope.launch(ioDispatcher) {
            trySafe { sessionHolder.getSafeActiveSession()?.roomService()?.joinRoom(room.roomId) }
        }
    }

    private fun retrieveTimelineEvents() {
        updateState { copy(roomInfo = room.getRoomInfo(currentName)) }
        trySafe {
            timeline = room.timelineService()
                .createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
                    removeListener(timelineListenerAdapter)
                    addListener(timelineListenerAdapter)
                    start()
                }
        }
    }

    private suspend fun handleTimelineEvents(events: List<TimelineEvent>) {
        Timber.tag(TAG).d("handleTimelineEvents:${events.size}")
        val stateEvent = room.stateService().getStateEvent("m.room.retention", QueryStringValue.IsEmpty)
        val maxLifetime = stateEvent?.content?.get("max_lifetime")?.toString()?.toDoubleOrNull()?.toLong()
        Timber.tag(TAG).d("maxLifetime:$maxLifetime")
        val displayableEvents = events.filter { it.isDisplayable(isSupportRoom, maxLifetime) }
            .filterNot { !debugMode && it.isNunchukErrorEvent() }
            .groupEvents(loadMore = ::handleLoadMore)
        val nunchukEvents = displayableEvents.filter(TimelineEvent::isNunchukEvent)
            .filterNot(TimelineEvent::isNunchukErrorEvent).sortedByDescending(TimelineEvent::time)
        val consumableEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
            .filterNot(NunchukMatrixEvent::isLocalEvent)
            .sortedByDescending(NunchukMatrixEvent::time)
        consumeEvents(consumableEvents)
    }

    private suspend fun consumeEvents(
        sortedEvents: List<NunchukMatrixEvent>
    ) {
        val unConsumeEvents = sortedEvents.filterNot { it.eventId in consumedEventIds }
        unConsumeEvents.chunked(20).asFlow().onStart {
            isConsumingEvents.set(true)
        }.map { events ->
            supervisorScope {
                val tasks = events.filterNot { it.eventId in consumedEventIds }
                    .map { event -> async { consumeEventUseCase(event) } }
                tasks.awaitAll()
            }
            consumedEventIds.addAll(events.map { event -> event.eventId })
        }.flowOn(ioDispatcher).onEach {
            onConsumeEventCompleted()
            handleUpdateMessagesContent()
        }.onCompletion {
            Timber.tag(TAG).d("onConsumeEventCompleted:${sortedEvents.size}")
            onConsumeEventCompleted()
            handleUpdateMessagesContent()
            isConsumingEvents.set(false)
        }.collect()
    }

    private fun onConsumeEventCompleted() {
        val latestEventTs = room.roomSummary().latestPreviewableEventTs()
        getTransactions()
        if (latestEventTs > latestPreviewableEventTs) {
            latestPreviewableEventTs = latestEventTs
            setEvent(HasUpdatedEvent)
        }
    }

    fun getTransactions() {
        val roomWallet = getState().roomWallet
        if (roomWallet != null && roomWallet.isCreated()) {
            getState().roomWallet?.walletId?.let {
                getTransactions(it)
                setEvent(GetRoomWalletSuccessEvent)
            }
        } else {
            getRoomWallet()
        }
    }

    private fun getTransactions(walletId: String) {
        val eventFilterNotLocal = timelineListenerAdapter.getNunchukEvents().filterNot {
            it.eventId.startsWith("\$local.")
        }.filter { consumedEventIds.contains(it.eventId) }
        viewModelScope.launch {
            val eventIds = mapTransactionEvents(eventFilterNotLocal)
            getTransactionsUseCase.execute(walletId, eventIds).flowOn(ioDispatcher)
                .onException { sendErrorEvent(it) }.collect { newTrans ->
                    if (newTrans != getState().transactions) {
                        updateState { copy(transactions = newTrans) }
                        handleUpdateMessagesContent(transactions = newTrans)
                    }
                }
        }
    }

    private fun mapTransactionEvents(events: List<TimelineEvent>) =
        events.filter { it.isInitTransactionEvent() || it.isReceiveTransactionEvent() }
            .map { it.eventId to it.isReceiveTransactionEvent() }

    fun handleSendMessage(content: String) {
        room.sendService().sendTextMessage(content)
    }

    fun handleTitleClick() {
        if (room.isDirectChat()) {
            setEvent(OpenChatInfoEvent)
        } else {
            setEvent(OpenChatGroupInfoEvent)
        }
    }

    val isSupportRoom: Boolean
        get() = getState().isSupportRoom

    fun handleLoadMore() {
        if (!isConsumingEvents.get() && timeline?.hasMoreToLoad(BACKWARDS).orFalse()) {
            isConsumingEvents.set(true)
            timeline?.paginate(BACKWARDS, PAGINATION)
        }
    }

    fun cancelWallet() {
        viewModelScope.launch {
            cancelWalletUseCase.execute(room.roomId).flowOn(ioDispatcher)
                .onException { sendErrorEvent(it) }.collect { getRoomWallet() }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            createSharedWalletUseCase.execute(room.roomId).flowOn(ioDispatcher)
                .onException { sendErrorEvent(it) }.collect {
                    getRoomWallet()
                    setEvent(RoomWalletCreatedEvent)
                }
        }
    }

    fun viewConfig() {
        getState().roomWallet?.jsonContent?.toRoomWalletData()?.let {
            setEvent(ViewWalletConfigEvent(room.roomId, it))
        }
    }

    fun denyWallet() {
        // FIXME
        cancelWallet()
    }

    fun handleAddEvent() {
        val roomWallet = getState().roomWallet
        if (roomWallet == null) {
            setEvent(CreateNewSharedWallet)
        } else if (roomWallet.walletId.isNotEmpty()) {
            viewModelScope.launch {
                getWalletUseCase.execute(walletId = roomWallet.walletId).flowOn(ioDispatcher)
                    .onException { sendErrorEvent(it) }.collect { onGetWallet(it.wallet) }
            }
        }
    }

    fun handleReceiveEvent() {
        val roomWallet = getState().roomWallet
        if (roomWallet == null) {
            setEvent(CreateNewSharedWallet)
        } else if (roomWallet.walletId.isNotEmpty()) {
            setEvent(ReceiveBTCEvent(roomWallet.walletId))
        }
    }

    private fun onGetWallet(wallet: Wallet) {
        if (wallet.balance.value > 0L) {
            setEvent(
                CreateNewTransaction(
                    roomId = room.roomId,
                    walletId = wallet.id,
                    availableAmount = wallet.balance.pureBTC()
                )
            )
        }
    }

    fun hideBannerNewChat() {
        viewModelScope.launch {
            hideBannerNewChatUseCase.execute().flowOn(ioDispatcher).catch { }
                .collect { setEvent(HideBannerNewChatEvent) }
        }
    }

    fun checkShowBannerNewChat() {
        viewModelScope.launch {
            checkShowBannerNewChatUseCase.execute().flowOn(ioDispatcher).onException { }.collect {
                if (!it) {
                    setEvent(HideBannerNewChatEvent)
                }
            }
        }
    }

    private fun sendErrorEvent(t: Throwable) {
        sendErrorEvent(room.roomId, t, sendErrorEventUseCase::execute)
    }

    fun markMessageRead(eventId: String) {
        viewModelScope.launch(ioDispatcher) {
            room.readService()
                .setReadReceipt(eventId = eventId, threadId = ReadService.THREAD_ID_MAIN)
        }
    }

    private fun handleUpdateMessagesContent(
        roomWallet: RoomWallet? = getState().roomWallet,
        transactions: List<TransactionExtended> = getState().transactions,
        isSelectedEnable: Boolean = getState().isSelectEnable
    ) {
        loadMessageJob?.cancel()
        loadMessageJob = viewModelScope.launch {
            val newMessages = withContext(ioDispatcher) {
                val stateEvent = room.stateService().getStateEvent("m.room.retention", QueryStringValue.IsEmpty)
                val maxLifetime = stateEvent?.content?.get("max_lifetime")?.toString()?.toDoubleOrNull()?.toLong()
                val displayableEvents = timelineListenerAdapter.getLastTimeEvents()
                    .filter { it.isDisplayable(isSupportRoom, maxLifetime) }
                    .filterNot { !debugMode && it.isNunchukErrorEvent() }
                    .groupEvents(loadMore = ::handleLoadMore)
                displayableEvents.toMessages(
                    currentId,
                    roomWallet,
                    transactions,
                    isSelectedEnable,
                    getState().selectedEventIds,
                    onCheckIsMyCoin = { walletId, address -> isMyCoin(walletId, address) }
                )
            }
            updateState {
                copy(
                    messages = newMessages,
                    transactions = transactions,
                    roomWallet = roomWallet,
                    isSelectEnable = isSelectedEnable
                )
            }
        }
    }

    fun isMyCoin(walletId: String, address: String) =
        runBlocking { isMyCoinUseCase(IsMyCoinUseCase.Param(walletId, address)) }.getOrDefault(
            false
        )

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

    fun sendMedia(uri: List<Uri>) {
        viewModelScope.launch {
            setEvent(Loading(true))
            val result = sendMediaUseCase(SendMediaUseCase.Data(room, uri))
            setEvent(Loading(false))
            if (result.isSuccess) {
                setEvent(OnSendMediaSuccess)
            } else {
                setEvent(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun getMediaMessages() = room.timelineService().getAttachmentMessages()

    fun clearEvent() {
        if (event.value != None) {
            setEvent(None)
        }
    }

    fun downloadOrOpen(message: NunchukFileMessage) {
        val mxcUrl = message.url
        val isLocalSendingFile = message.isMine && mxcUrl.startsWith("content://")
        if (isLocalSendingFile) {
            tryOrNull { Uri.parse(mxcUrl) }?.let {
                setEvent(OpenFile(it, message.mimeType))
            }
        } else {
            viewModelScope.launch {
                val fileService =
                    sessionHolder.getSafeActiveSession()?.fileService() ?: return@launch
                val fileState = fileService.fileState(
                    mxcUrl, message.filename, message.mimeType, message.elementToDecrypt
                )
                var canOpen =
                    fileState is FileService.FileState.InCache && fileState.decryptedFileInCache
                if (!canOpen) {
                    // First download, or download and decrypt, or decrypt from cache
                    val result = runCatching {
                        fileService.downloadFile(message)
                    }
                    canOpen = result.isSuccess
                }
                if (canOpen) {
                    fileService.getTemporarySharableURI(
                        mxcUrl, message.filename, message.mimeType, message.elementToDecrypt
                    )?.let { uri ->
                        setEvent(OpenFile(uri, message.mimeType))
                    }
                }
            }
        }
    }

    fun isByzantineChat() = getState().isHasByzantineGroup || getState().isGroupChatRoom

    fun isShowCollaborativeWallet(): Boolean =
        getState().matrixBasedCollaborativeWalletEnabled == true

    override fun onCleared() {
        timeline?.apply {
            dispose()
            removeAllListeners()
        }
        sessionHolder.clearActiveRoom()
        consumedEventIds.clear()
        super.onCleared()
    }

    companion object {
        private const val TAG = "RoomDetailViewModel"
    }

}
