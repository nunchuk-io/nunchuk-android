package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.list.isServerNotices
import com.nunchuk.android.messages.util.isGroupMembershipRequestEvent
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupWalletKeyHealthStatusUseCase
import com.nunchuk.android.usecase.byzantine.KeyHealthCheckUseCase
import com.nunchuk.android.usecase.byzantine.RequestHealthCheckUseCase
import com.nunchuk.android.usecase.membership.CreateOrUpdateGroupChatUseCase
import com.nunchuk.android.usecase.membership.DismissAlertUseCase
import com.nunchuk.android.usecase.membership.GetAlertGroupUseCase
import com.nunchuk.android.usecase.membership.GetGroupChatUseCase
import com.nunchuk.android.usecase.membership.GetHistoryPeriodUseCase
import com.nunchuk.android.usecase.membership.MarkAlertAsReadUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import javax.inject.Inject

@HiltViewModel
class GroupDashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder,
    private val getGroupUseCase: GetGroupUseCase,
    private val getAlertGroupUseCase: GetAlertGroupUseCase,
    private val getGroupChatUseCase: GetGroupChatUseCase,
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase,
    private val dismissAlertUseCase: DismissAlertUseCase,
    private val markAlertAsReadUseCase: MarkAlertAsReadUseCase,
    private val getHistoryPeriodUseCase: GetHistoryPeriodUseCase,
    private val getGroupWalletKeyHealthStatusUseCase: GetGroupWalletKeyHealthStatusUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cardIdManager: CardIdManager,
    private val requestHealthCheckUseCase: RequestHealthCheckUseCase,
    private val keyHealthCheckUseCase: KeyHealthCheckUseCase
) : ViewModel() {

    private val args = GroupDashboardFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupDashboardEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(GroupDashboardState())
    val state = _state.asStateFlow()

    private var timeline: Timeline? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()
    private var loadAlertJob: Job? = null

    init {
        loadActiveSession()
        viewModelScope.launch {
            timelineListenerAdapter.data.collect(::handleTimelineEvents)
        }
        getGroup()
        val walletId = args.walletId
        if (!walletId.isNullOrEmpty()) {
            getWallet(walletId)
            getKeysStatus(walletId)
        }
        getAlerts()
        getGroupChat()
    }

    private fun getKeysStatus(walletId: String) {
        viewModelScope.launch {
            getGroupWalletKeyHealthStatusUseCase(
                GetGroupWalletKeyHealthStatusUseCase.Params(
                    args.groupId,
                    walletId
                )
            )
                .onSuccess { status ->
                    _state.update { state ->
                        state.copy(keyStatus = status.associateBy { it.xfp })
                    }
                }
        }
    }

    fun getAlerts() {
        if (loadAlertJob?.isActive == true) return
        loadAlertJob = viewModelScope.launch {
            val result = getAlertGroupUseCase(args.groupId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(alerts = result.getOrDefault(emptyList()))
            }
        }
    }

    private fun getGroupChat() = viewModelScope.launch {
        val result = getGroupChatUseCase(args.groupId)
        if (result.isSuccess) {
            _state.value = _state.value.copy(groupChat = result.getOrNull())
        }
    }

    private fun getWallet(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                val signers = wallet.signers
                    .filter { signer -> signer.type != SignerType.SERVER }
                    .map { signer -> signer.toModel() }
                    .map { signer ->
                        if (signer.type == SignerType.NFC) signer.copy(
                            cardId = cardIdManager.getCardId(
                                signer.id
                            )
                        ) else signer
                    }
                    .toList()

                _state.update { state -> state.copy(wallet = wallet, signers = signers) }
            }
        }
    }

    private fun getGroup() {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            val group = getGroupUseCase(args.groupId).getOrNull()
            _event.emit(GroupDashboardEvent.Loading(false))
            val members = group?.members.orEmpty()
            _state.update { it.copy(group = group, myRole = currentUserRole(members)) }
        }
    }

    fun isEnableStartGroupChat(): Boolean {
        val members = state.value.group?.members ?: emptyList()
        return members.count { it.isContact() } == 2
    }

    private fun loadActiveSession() {
        sessionHolder.getSafeActiveSession()?.let { session ->
            session.roomService().getRoomSummaries(roomSummaryQueryParams {
                memberships = Membership.activeMemberships()
            }).find { roomSummary ->
                roomSummary.isServerNotices()
            }?.let {
                viewModelScope.launch(ioDispatcher) {
                    runCatching {
                        session.roomService().joinRoom(it.roomId)
                        session.roomService().getRoom(it.roomId)
                            ?.let(::retrieveTimelineEvents)
                    }
                }
            }
        }
    }

    private fun retrieveTimelineEvents(room: Room) {
        timeline = room.timelineService()
            .createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
                removeAllListeners()
                addListener(timelineListenerAdapter)
                start()
            }
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        events.findLast(TimelineEvent::isGroupMembershipRequestEvent)?.let { getGroup() }
    }

    fun getMembers(): List<ByzantineMember> {
        return state.value.group?.members ?: emptyList()
    }

    fun setByzantineMembers(members: List<ByzantineMember>) {
        _state.update {
            it.copy(
                group = it.group?.copy(members = members)
            )
        }
    }

    fun getByzantineGroup(): ByzantineGroup? {
        return state.value.group
    }

    private fun currentUserRole(members: List<ByzantineMember>): AssistedWalletRole {
        return members.firstOrNull {
            it.emailOrUsername == accountManager.getAccount().email
                    || it.emailOrUsername == accountManager.getAccount().username
        }?.role.toRole
    }

    fun groupChat(): GroupChat? {
        return state.value.groupChat
    }

    fun createGroupChat() {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            val result = createOrUpdateGroupChatUseCase(
                CreateOrUpdateGroupChatUseCase.Param(args.groupId)
            )
            _event.emit(GroupDashboardEvent.Loading(false))
            if (result.isSuccess) {
                _state.value = _state.value.copy(groupChat = result.getOrNull())
                _event.emit(GroupDashboardEvent.NavigateToGroupChat)
            } else {
                _event.emit(GroupDashboardEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun getGroupChatHistoryPeriod() {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            val result = getHistoryPeriodUseCase(Unit)
            _event.emit(GroupDashboardEvent.Loading(false))
            if (result.isSuccess) {
                val periods = result.getOrDefault(emptyList())
                if (periods.isNotEmpty()) {
                    _event.emit(GroupDashboardEvent.GetHistoryPeriodSuccess(periods))
                }
            } else {
                _event.emit(GroupDashboardEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun updateGroupChatHistoryPeriod(historyPeriod: HistoryPeriod?) {
        historyPeriod ?: return
        val groupChat = _state.value.groupChat ?: return
        _state.value = _state.value.copy(groupChat = groupChat.copy(historyPeriod = historyPeriod))
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            val result = dismissAlertUseCase(DismissAlertUseCase.Param(alertId, args.groupId))
            if (result.isSuccess) {
                _state.update {
                    it.copy(alerts = it.alerts.filterNot { alert -> alert.id == alertId })
                }
            } else {
                _event.emit(GroupDashboardEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun markAsReadAlert(alertId: String) {
        viewModelScope.launch {
            markAlertAsReadUseCase(MarkAlertAsReadUseCase.Param(alertId, args.groupId))
        }
    }

    fun onRequestHealthCheck(signerModel: SignerModel) {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            requestHealthCheckUseCase(
                RequestHealthCheckUseCase.Params(
                    args.groupId,
                    args.walletId.orEmpty(),
                    signerModel.fingerPrint,
                )
            ).onSuccess {
                _event.emit(GroupDashboardEvent.RequestHealthCheckSuccess)
            }
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }

    fun onHealthCheck(signerModel: SignerModel) {
        viewModelScope.launch { 
            _event.emit(GroupDashboardEvent.Loading(true))
            keyHealthCheckUseCase(
                KeyHealthCheckUseCase.Params(
                    args.groupId,
                    args.walletId.orEmpty(),
                    signerModel.fingerPrint,
                )
            ).onSuccess {
                _event.emit(GroupDashboardEvent.GetHealthCheckPayload(it))
            }
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }
}