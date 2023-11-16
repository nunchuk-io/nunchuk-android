package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesInheritanceUseCase
import com.nunchuk.android.core.domain.membership.RecoverKeyUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUseCase
import com.nunchuk.android.core.domain.membership.RequestPlanningInheritanceUserDataUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.isColdCard
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.components.tabs.services.keyrecovery.intro.KeyRecoveryIntroEvent
import com.nunchuk.android.messages.components.list.isServerNotices
import com.nunchuk.android.messages.util.isGroupMembershipRequestEvent
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupWalletKeyHealthStatusRemoteUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupWalletKeyHealthStatusUseCase
import com.nunchuk.android.usecase.byzantine.KeyHealthCheckUseCase
import com.nunchuk.android.usecase.byzantine.RequestHealthCheckUseCase
import com.nunchuk.android.usecase.membership.DismissAlertUseCase
import com.nunchuk.android.usecase.membership.GetAlertGroupRemoteUseCase
import com.nunchuk.android.usecase.membership.GetAlertGroupUseCase
import com.nunchuk.android.usecase.membership.GetGroupChatUseCase
import com.nunchuk.android.usecase.membership.GetHistoryPeriodUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.MarkAlertAsReadUseCase
import com.nunchuk.android.usecase.membership.MarkSetupInheritanceUseCase
import com.nunchuk.android.usecase.membership.RestartWizardUseCase
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import com.nunchuk.android.usecase.user.SetRegisterColdcardUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder,
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    private val getAlertGroupUseCase: GetAlertGroupUseCase,
    private val getAlertGroupRemoteUseCase: GetAlertGroupRemoteUseCase,
    private val getGroupChatUseCase: GetGroupChatUseCase,
    private val dismissAlertUseCase: DismissAlertUseCase,
    private val markAlertAsReadUseCase: MarkAlertAsReadUseCase,
    private val getHistoryPeriodUseCase: GetHistoryPeriodUseCase,
    private val getGroupWalletKeyHealthStatusUseCase: GetGroupWalletKeyHealthStatusUseCase,
    private val getGroupWalletKeyHealthStatusRemoteUseCase: GetGroupWalletKeyHealthStatusRemoteUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cardIdManager: CardIdManager,
    private val requestHealthCheckUseCase: RequestHealthCheckUseCase,
    private val keyHealthCheckUseCase: KeyHealthCheckUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val setRegisterColdcardUseCase: SetRegisterColdcardUseCase,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase,
    private val requestPlanningInheritanceUserDataUseCase: RequestPlanningInheritanceUserDataUseCase,
    private val calculateRequiredSignaturesInheritanceUseCase: CalculateRequiredSignaturesInheritanceUseCase,
    private val requestPlanningInheritanceUseCase: RequestPlanningInheritanceUseCase,
    private val restartWizardUseCase: RestartWizardUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val markSetupInheritanceUseCase: MarkSetupInheritanceUseCase,
    private val recoverKeyUseCase: RecoverKeyUseCase,
    private val getGroupDummyTransactionPayloadUseCase: GetGroupDummyTransactionPayloadUseCase,
) : ViewModel() {

    private val args = GroupDashboardFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val walletId =
        savedStateHandle.getStateFlow<String?>(EXTRA_WALLET_ID, args.walletId.orEmpty())

    private val _event = MutableSharedFlow<GroupDashboardEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(GroupDashboardState())
    val state = _state.asStateFlow()

    private var timeline: Timeline? = null

    private val timelineListenerAdapter = TimelineListenerAdapter()

    init {
        viewModelScope.launch {
            getGroupUseCase(
                GetGroupUseCase.Params(
                    args.groupId
                )
            )
                .map { it.getOrElse { null } }
                .distinctUntilChanged()
                .collect { group ->
                    val members = group?.members.orEmpty()
                    _state.update { it.copy(group = group, myRole = currentUserRole(members)) }
                }
        }
        viewModelScope.launch {
            getGroupWalletKeyHealthStatusUseCase(
                GetGroupWalletKeyHealthStatusUseCase.Params(
                    args.groupId,
                    walletId.value.orEmpty(),
                )
            )
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { keyStatus ->
                    _state.update { state ->
                        state.copy(keyStatus = keyStatus.associateBy { it.xfp })
                    }
                }
        }
        viewModelScope.launch {
            walletId.collect { walletId ->
                if (!walletId.isNullOrEmpty()) {
                    getWallet(walletId)
                    getKeysStatus()
                }
            }
        }
        loadActiveSession()
        viewModelScope.launch {
            timelineListenerAdapter.data.collect(::handleTimelineEvents)
        }
        getGroupChat()
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { wallets ->
                    wallets.find { wallet -> wallet.groupId == args.groupId }?.let { wallet ->
                        _state.update { state -> state.copy(isSetupInheritance = wallet.isSetupInheritance) }
                        savedStateHandle[EXTRA_WALLET_ID] = wallet.localId
                    }
                    _state.update {
                        it.copy(
                            isSetupInheritance = wallets.find { wallet -> wallet.groupId == args.groupId }?.isSetupInheritance.orFalse(),
                            inheritanceOwnerId = wallets.find { wallet -> wallet.groupId == args.groupId }?.ext?.inheritanceOwnerId)
                    }
                }
        }
        viewModelScope.launch {
            getAlertGroupUseCase(
                GetAlertGroupUseCase.Params(
                    groupId = args.groupId
                )
            )
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { alerts ->
                    val groupWalletSetupAlert = alerts.find { alert -> alert.type == AlertType.GROUP_WALLET_SETUP }
                    if (groupWalletSetupAlert != null && getWalletId().isNotEmpty()) {
                        dismissAlert(groupWalletSetupAlert.id)
                    }
                    _state.update { state ->
                        state.copy(alerts = alerts)
                    }
                }
        }
        viewModelScope.launch {
            getInheritanceUseCase(
                GetInheritanceUseCase.Param(
                    walletId.value.orEmpty(),
                    args.groupId
                )
            ).onSuccess { inheritance ->
                _state.update {
                    it.copy(isHasPendingRequestInheritance = inheritance.pendingRequests.isNotEmpty(), inheritanceOwnerId = inheritance.ownerId)
                }
            }
        }
        getGroup()
        getAlerts()
    }

    fun getKeysStatus() {
        if (walletId.value.isNullOrEmpty()) return
        viewModelScope.launch {
            getGroupWalletKeyHealthStatusRemoteUseCase(
                GetGroupWalletKeyHealthStatusRemoteUseCase.Params(
                    args.groupId,
                    walletId.value.orEmpty(),
                )
            ).onSuccess { result ->
                _state.update { state ->
                    state.copy(keyStatus = result.associateBy { it.xfp })
                }
            }
        }
    }

    fun getAlerts() {
        viewModelScope.launch {
            getAlertGroupRemoteUseCase(
                GetAlertGroupRemoteUseCase.Params(
                    groupId = args.groupId
                )
            )
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
            getGroupRemoteUseCase(
                GetGroupRemoteUseCase.Params(
                    args.groupId
                )
            )
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }

    fun isEnableStartGroupChat(): Boolean {
        val members = state.value.group?.members ?: emptyList()
        return members.count { it.isContact() } >= 2
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

    fun isPendingCreateWallet(): Boolean {
        return state.value.group?.isPendingWallet() == true
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
        _state.update {
            it.copy(groupChat = groupChat.copy(historyPeriod = historyPeriod))
        }
    }

    fun updateGroupChat(groupChat: GroupChat) {
        _state.update {
            it.copy(groupChat = groupChat)
        }
    }

    fun dismissCurrentAlert() {
        currentSelectedAlert?.let { alert ->
            dismissAlert(alert.id)
        }
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

    fun confirmPassword(
        password: String,
        targetAction: TargetAction
    ) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(GroupDashboardEvent.Loading(true))
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = targetAction.name,
                password = password
            )
        )
        _event.emit(GroupDashboardEvent.Loading(false))
        if (result.isSuccess) {
            val token = result.getOrThrow().orEmpty()
            when (targetAction) {
                TargetAction.UPDATE_INHERITANCE_PLAN -> getInheritance(token, false)
                TargetAction.UPDATE_SERVER_KEY -> {
                    state.value.signers.find { it.type == SignerType.SERVER }?.let { signer ->
                        _event.emit(
                            GroupDashboardEvent.UpdateServerKey(
                                token,
                                signer,
                                args.groupId
                            )
                        )
                    }
                }

                TargetAction.EMERGENCY_LOCKDOWN -> _event.emit(
                    GroupDashboardEvent.OpenEmergencyLockdown(
                        token
                    )
                )

                else -> {}
            }
        } else {
            _event.emit(GroupDashboardEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getInheritance(token: String, isAlertFlow: Boolean) = viewModelScope.launch {
        getInheritanceUseCase(
            GetInheritanceUseCase.Param(
                walletId.value.orEmpty(),
                args.groupId
            )
        ).onSuccess { inheritance ->
            _state.update {
                it.copy(inheritanceOwnerId = inheritance.ownerId, isHasPendingRequestInheritance = inheritance.pendingRequests.isNotEmpty())
            }
            _event.emit(GroupDashboardEvent.GetInheritanceSuccess(inheritance, token, isAlertFlow))
        }.onFailure {
            _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
        }
    }

    fun onRequestHealthCheck(signerModel: SignerModel) {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            requestHealthCheckUseCase(
                RequestHealthCheckUseCase.Params(
                    args.groupId,
                    walletId.value.orEmpty(),
                    signerModel.fingerPrint,
                )
            ).onSuccess {
                _event.emit(GroupDashboardEvent.RequestHealthCheckSuccess)
            }.onFailure {
                _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
            }
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }

    fun onHealthCheck(signerModel: SignerModel) {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            keyHealthCheckUseCase(
                KeyHealthCheckUseCase.Params(
                    groupId = args.groupId,
                    walletId = walletId.value.orEmpty(),
                    xfp = signerModel.fingerPrint,
                    draft = true
                )
            ).onSuccess {
                _event.emit(GroupDashboardEvent.GetHealthCheckPayload(it))
            }.onFailure {
                _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
            }
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }

    fun getWalletId() = walletId.value.orEmpty()
    fun handleRegisterSigners(xfps: List<String>) {
        viewModelScope.launch {
            val signers = _state.value.wallet.signers.filter { it.masterFingerprint in xfps }
            val totalColdcard = signers.count { it.isColdCard }
            val totalAirgap = signers.count { it.type == SignerType.AIRGAP && !it.isColdCard }
            if (totalColdcard > 0) {
                setRegisterColdcardUseCase(
                    SetRegisterColdcardUseCase.Params(
                        walletId.value.orEmpty(),
                        totalColdcard
                    )
                )
            }
            if (totalAirgap > 0) {
                setRegisterAirgapUseCase(
                    SetRegisterAirgapUseCase.Params(
                        walletId.value.orEmpty(),
                        1
                    )
                )
            }
            if (totalColdcard > 0 || totalAirgap > 0) {
                _event.emit(GroupDashboardEvent.RegisterSignersSuccess(totalColdcard, totalAirgap))
            }
        }
    }


    fun setCurrentSelectedAlert(alert: Alert) {
        savedStateHandle[EXTRA_SELECTED_ALERT] = alert
    }

    fun calculateRequiredSignatures() {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            val userData = requestPlanningInheritanceUserDataUseCase(
                RequestPlanningInheritanceUserDataUseCase.Param(
                    walletId = getWalletId(),
                    groupId = args.groupId
                )
            )
            calculateRequiredSignaturesInheritanceUseCase(
                CalculateRequiredSignaturesInheritanceUseCase.Param(
                    walletId = getWalletId(),
                    action = CalculateRequiredSignaturesAction.REQUEST_PLANNING,
                    groupId = args.groupId
                )
            ).onSuccess { resultCalculate ->
                requestPlanningInheritanceUseCase(
                    RequestPlanningInheritanceUseCase.Param(
                        userData = userData.getOrThrow(),
                        walletId = getWalletId(),
                        groupId = args.groupId
                    )
                ).onSuccess {
                    _state.update { state ->
                        state.copy(isHasPendingRequestInheritance = true, inheritanceOwnerId = accountManager.getAccount().id)
                    }
                    _event.emit(
                        GroupDashboardEvent.CalculateRequiredSignaturesSuccess(
                            type = resultCalculate.type,
                            walletId = getWalletId(),
                            userData = userData.getOrThrow(),
                            requiredSignatures = resultCalculate.requiredSignatures,
                            dummyTransactionId = it
                        )
                    )
                }.onFailure {
                    _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
            }
            _event.emit(GroupDashboardEvent.Loading(false))
        }
    }

    fun restartWizard() {
        viewModelScope.launch {
            restartWizardUseCase(
                RestartWizardUseCase.Param(
                    plan = membershipStepManager.plan,
                    groupId = args.groupId
                )
            ).onSuccess {
                _event.emit(GroupDashboardEvent.RestartWizardSuccess)
            }.onFailure {
                _event.emit(GroupDashboardEvent.Error(it.message.orUnknownError()))
            }
        }
    }

    fun markSetupInheritance(type: DummyTransactionType) = viewModelScope.launch {
        markSetupInheritanceUseCase(
            MarkSetupInheritanceUseCase.Param(
                walletId = getWalletId(),
                isSetupInheritance = type != DummyTransactionType.CANCEL_INHERITANCE_PLAN
            )
        )
    }

    fun recoverKey(xfp: String) {
        viewModelScope.launch {
            _event.emit(GroupDashboardEvent.Loading(true))
            val result = recoverKeyUseCase(
                RecoverKeyUseCase.Param(
                    xfp = xfp
                )
            )
            _event.emit(GroupDashboardEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(GroupDashboardEvent.DownloadBackupKeySuccess(result.getOrThrow()))
            } else {
                _event.emit(GroupDashboardEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun getGroupDummyTransactionPayload(dummyTransactionId: String) = viewModelScope.launch {
        getGroupDummyTransactionPayloadUseCase(
            GetGroupDummyTransactionPayloadUseCase.Param(
                groupId = args.groupId,
                walletId = getWalletId(),
                transactionId = dummyTransactionId
            )
        ).onSuccess {
            _event.emit(GroupDashboardEvent.GroupDummyTransactionPayloadSuccess(it))
        }
    }

    fun isInheritanceOwner(): Boolean {
        return  _state.value.inheritanceOwnerId.isNullOrEmpty() || _state.value.inheritanceOwnerId == accountManager.getAccount().id
    }

    private val currentSelectedAlert: Alert?
        get() = savedStateHandle.get<Alert>(EXTRA_SELECTED_ALERT)

    companion object {
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_SELECTED_ALERT = "alert"
    }
}