package com.nunchuk.android.main.components.tabs.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicesTabViewModel @Inject constructor(
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getServerWalletUseCase: GetServerWalletUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ServicesTabEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ServicesTabState())
    val state = _state.asStateFlow()

    init {
        getUserSubscription()
    }

    private fun getUserSubscription() = viewModelScope.launch {
        val result = getUserSubscriptionUseCase(Unit)
        if (result.isSuccess) {
            val subscription = result.getOrThrow()
            val getServerWalletResult = getServerWalletUseCase(Unit)
            if (getServerWalletResult.isFailure) return@launch
            val walletLocalId =
                getServerWalletResult.getOrThrow().planWalletCreated[subscription.slug].orEmpty()
            val isPremiumUser = subscription.subscriptionId.isNullOrEmpty().not()
            _state.update {
                it.copy(
                    isCreatedAssistedWallet = walletLocalId.isNotEmpty(),
                    isPremiumUser = isPremiumUser,
                    plan = subscription.plan,
                    rowItems = it.initRowItems(subscription.plan)
                )
            }
        } else {
            _state.update {
                it.copy(rowItems = it.initRowItems(MembershipPlan.NONE))
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

    fun getGroupStage(): MembershipStage {
        if (_state.value.isCreatedAssistedWallet) return MembershipStage.DONE
        if (membershipStepManager.isNotConfig()) return MembershipStage.NONE
        return MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
    }
}