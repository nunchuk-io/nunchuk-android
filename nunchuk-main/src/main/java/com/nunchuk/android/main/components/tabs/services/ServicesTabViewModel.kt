package com.nunchuk.android.main.components.tabs.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlanFlowUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.messages.usecase.message.GetOrCreateSupportRoomUseCase
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.InheritanceCheckUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
    private val getLocalMembershipPlanFlowUseCase: GetLocalMembershipPlanFlowUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getOrCreateSupportRoomUseCase: GetOrCreateSupportRoomUseCase,
    private val inheritanceCheckUseCase: InheritanceCheckUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<ServicesTabEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ServicesTabState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlanFlowUseCase(Unit)
                .map { it.getOrElse { MembershipPlan.NONE } }
                .zip(getAssistedWalletIdsFlowUseCase(Unit).map { it.getOrElse { "" } }
                    .distinctUntilChanged()) { plan, walletId ->
                    plan to walletId
                }.collect { pair ->
                    getInheritance(pair.second, pair.first)
                }
        }
    }

    private suspend fun getInheritance(walletLocalId: String, plan: MembershipPlan) {
        if (walletLocalId.isNotEmpty() && plan == MembershipPlan.HONEY_BADGER) {
            val result = getInheritanceUseCase(walletLocalId)
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        plan = plan,
                        isPremiumUser = true,
                        rowItems = it.initRowItems(plan, result.getOrThrow()),
                        inheritance = result.getOrThrow(),
                    )
                }
            } else {
                _event.emit(ServicesTabEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        } else {
            _state.update {
                it.copy(
                    plan = plan,
                    isPremiumUser = plan != MembershipPlan.NONE,
                    rowItems = it.initRowItems(plan),
                )
            }
        }
    }

    fun confirmPassword(password: String, item: ServiceTabRowItem) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(ServicesTabEvent.Loading(true))
        val targetAction = when (item) {
            is ServiceTabRowItem.EmergencyLockdown -> {
                VerifiedPasswordTargetAction.EMERGENCY_LOCKDOWN.name
            }
            is ServiceTabRowItem.CoSigningPolicies -> {
                VerifiedPasswordTargetAction.UPDATE_SERVER_KEY.name
            }
            is ServiceTabRowItem.ViewInheritancePlan -> {
                VerifiedPasswordTargetAction.UPDATE_INHERITANCE_PLAN.name
            }
            else -> {
                throw IllegalArgumentException()
            }
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = targetAction,
                password = password
            )
        )
        _event.emit(ServicesTabEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(ServicesTabEvent.CheckPasswordSuccess(result.getOrThrow().orEmpty(), item))
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getServiceKey(token: String) = viewModelScope.launch {
        _event.emit(ServicesTabEvent.Loading(true))
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val walletId = it.getOrNull() ?: return@collect
            getWalletUseCase.execute(walletId)
                .map { it.wallet.signers }
                .map { signers -> signers.firstOrNull { it.type == SignerType.SERVER } }
                .flowOn(Dispatchers.IO)
                .onException {
                    _event.emit(ServicesTabEvent.Loading(false))
                    _event.emit(ServicesTabEvent.ProcessFailure(it.message.orUnknownError()))
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    _event.emit(ServicesTabEvent.Loading(false))
                    it?.let {
                        _event.emit(
                            ServicesTabEvent.GetServerKeySuccess(
                                signer = it,
                                walletId = walletId,
                                token = token
                            )
                        )
                    }
                }
        }
    }

    fun checkInheritance() = viewModelScope.launch {
        val magic = _state.value.inheritance?.magic
        if (magic.isNullOrEmpty()) return@launch
        _event.emit(ServicesTabEvent.Loading(true))
        val result = inheritanceCheckUseCase(magic)
        _event.emit(ServicesTabEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(ServicesTabEvent.CheckInheritance(result.getOrThrow()))
        } else {
            _event.emit(ServicesTabEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getInheritance() = _state.value.inheritance

    fun getGroupStage(): MembershipStage {
        if (_state.value.isCreatedAssistedWallet) return MembershipStage.DONE
        if (membershipStepManager.isNotConfig()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }

    fun getOrCreateSupportRom() {
        viewModelScope.launch {
            _event.emit(ServicesTabEvent.LoadingEvent(true))
            val result = getOrCreateSupportRoomUseCase(Unit)
            _event.emit(ServicesTabEvent.LoadingEvent(false))
            if (result.isSuccess) {
                _event.emit(ServicesTabEvent.CreateSupportRoomSuccess(result.getOrThrow().roomId))
            } else {
                _event.emit(ServicesTabEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}