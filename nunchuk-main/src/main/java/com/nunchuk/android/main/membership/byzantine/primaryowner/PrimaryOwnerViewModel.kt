package com.nunchuk.android.main.membership.byzantine.primaryowner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.UpdatePrimaryOwnerUseCase
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.isColdCard
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.CreateGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrimaryOwnerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val updatePrimaryOwnerUseCase: UpdatePrimaryOwnerUseCase,
    private val createGroupWalletUseCase: CreateGroupWalletUseCase,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<PrimaryOwnerEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(PrimaryOwnerState())
    val state = _state.asStateFlow()

    private val args = PrimaryOwnerFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        if (args.walletId.isNullOrEmpty().not()) {
            viewModelScope.launch {
                getAssistedWalletsFlowUseCase(Unit)
                    .collect { wallets ->
                        val wallet = wallets.getOrNull()?.firstOrNull { it.localId == args.walletId }
                        _state.update { it.copy(wallet = wallet) }
                        setPreviousEmail()
                    }
            }
        }
        viewModelScope.launch {
            getGroupUseCase(GetGroupUseCase.Params(args.groupId))
                .collect { group ->
                    _state.update { it.copy(members = group.getOrNull()?.members.orEmpty().filter { it.role != AssistedWalletRole.OBSERVER.name }) }
                    setPreviousEmail()
                }
        }
    }

    private fun setPreviousEmail() {
        state.value.wallet?.let { wallet ->
            state.value.members.firstOrNull { it.membershipId == wallet.primaryMembershipId }?.user?.email?.let { email ->
                _state.update { it.copy(email = email, previousEmail = email) }
            }
        }
    }

    fun onContinueClick() {
        if (args.flow == PrimaryOwnerFlow.EDIT) {
            updatePrimaryOwner()
        } else {
            createGroupWallet(args.groupId)
        }
    }

    fun createGroupWallet(groupId: String) {
        if (args.walletName.isNullOrEmpty()) return
        val membershipId = getMembershipId()
        viewModelScope.launch {
            _event.emit(PrimaryOwnerEvent.Loading(true))
            createGroupWalletUseCase(
                CreateGroupWalletUseCase.Param(
                    name = args.walletName!!,
                    groupId = groupId,
                    primaryMembershipId = membershipId
                )
            ).onSuccess {
                val totalAirgap = it.signers.count { signer -> signer.type == SignerType.AIRGAP && !signer.isColdCard }
                if (totalAirgap > 0) {
                    setRegisterAirgapUseCase(SetRegisterAirgapUseCase.Params(it.id, totalAirgap))
                }
                _event.emit(
                    PrimaryOwnerEvent.OnCreateWalletSuccess(
                        walletId = it.id,
                        airgapCount = totalAirgap
                    )
                )
            }.onFailure {
                _event.emit(
                    PrimaryOwnerEvent.Error(it.message.orUnknownError())
                )
            }
            _event.emit(PrimaryOwnerEvent.Loading(false))
        }
    }

    private fun updatePrimaryOwner() {
        val membershipId = getMembershipId()
        if (membershipId.isNullOrEmpty() || args.walletId.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch {
            _event.emit(PrimaryOwnerEvent.Loading(true))
            val result = updatePrimaryOwnerUseCase(
                UpdatePrimaryOwnerUseCase.Param(
                    args.groupId,
                    args.walletId!!,
                    membershipId
                )
            )
            _event.emit(PrimaryOwnerEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(PrimaryOwnerEvent.UpdatePrimaryOwnerSuccess)
            } else {
                _event.emit(PrimaryOwnerEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email) }
    }

    private fun getMembershipId(): String? {
        return state.value.members.firstOrNull { it.user?.email == state.value.email }?.membershipId
    }

    fun enableContinueButton(): Boolean {
        return state.value.email.isNotEmpty() && args.flow == PrimaryOwnerFlow.SETUP
                || state.value.email.isNotEmpty() && state.value.email != state.value.previousEmail && args.flow == PrimaryOwnerFlow.EDIT
    }
}

data class PrimaryOwnerState(
    val members: List<ByzantineMember> = emptyList(),
    val email: String = "",
    val previousEmail: String = "",
    val wallet: AssistedWalletBrief? = null,
)

sealed class PrimaryOwnerEvent {
    data class Loading(val isLoading: Boolean) : PrimaryOwnerEvent()
    data class Error(val message: String) : PrimaryOwnerEvent()
    data object UpdatePrimaryOwnerSuccess : PrimaryOwnerEvent()
    data class OnCreateWalletSuccess(val walletId: String, val airgapCount: Int) : PrimaryOwnerEvent()
}