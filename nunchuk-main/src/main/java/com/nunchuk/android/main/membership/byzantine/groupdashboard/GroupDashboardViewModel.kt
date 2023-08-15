package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
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
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.membership.CreateOrUpdateGroupChatUseCase
import com.nunchuk.android.usecase.membership.DismissAlertUseCase
import com.nunchuk.android.usecase.membership.GetAlertGroupUseCase
import com.nunchuk.android.usecase.membership.GetGroupChatUseCase
import com.nunchuk.android.usecase.membership.GetHistoryPeriodUseCase
import com.nunchuk.android.usecase.membership.MarkAlertAsReadUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
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
    private val getWalletUseCase: GetWalletUseCase,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder,
    private val getGroupUseCase: GetGroupUseCase,
    private val getAlertGroupUseCase: GetAlertGroupUseCase,
    private val getGroupChatUseCase: GetGroupChatUseCase,
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase,
    private val dismissAlertUseCase: DismissAlertUseCase,
    private val markAlertAsReadUseCase: MarkAlertAsReadUseCase,
    private val getHistoryPeriodUseCase: GetHistoryPeriodUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
        if (!args.walletId.isNullOrEmpty()) {
            getWallet(args.walletId)
        }
        getAlerts()
        getGroupChat()
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
            getWalletUseCase.execute(walletId)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    _state.value = _state.value.copy(
                        walletExtended = it
                    )
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
        return members.firstOrNull { it.emailOrUsername == accountManager.getAccount().email
                || it.emailOrUsername == accountManager.getAccount().username }?.role.toRole
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
                    it.copy(alerts = it.alerts.filterNot { it.id == alertId })
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

}