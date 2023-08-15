package com.nunchuk.android.main.membership.byzantine.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.usecase.byzantine.GetGroupBriefByIdFlowUseCase
import com.nunchuk.android.usecase.user.IsViewPendingGroupUseCase
import com.nunchuk.android.usecase.user.SetViewPendingGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupPendingIntroViewModel @Inject constructor(
    private val getGroupBriefByIdFlowUseCase: GetGroupBriefByIdFlowUseCase,
    private val isViewPendingGroupUseCase: IsViewPendingGroupUseCase,
    private val setViewPendingGroupUseCase: SetViewPendingGroupUseCase,
    private val accountManager: AccountManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = GroupPendingIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(GroupPendingIntroUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            isViewPendingGroupUseCase(args.groupId).onSuccess { isView ->
                val groupBrief =
                    getGroupBriefByIdFlowUseCase(args.groupId).map { it.getOrNull() }.firstOrNull()
                val email = accountManager.getAccount().email
                _state.update {
                    it.copy(
                        isViewPendingWallet = isView,
                        masterName = groupBrief?.getMasterName().orEmpty(),
                        role = groupBrief?.members?.find { member -> member.emailOrUsername == email }?.role.toRole
                    )
                }
                if (!isView) {
                    setViewPendingGroupUseCase(args.groupId)
                }
            }
        }
    }

    fun markHandleViewPendingWallet() {
        _state.update { it.copy(isViewPendingWallet = false) }
    }

    fun isMasterOrAdmin() = state.value.role.isMasterOrAdmin
}

data class GroupPendingIntroUiState(
    val masterName: String = "",
    val isViewPendingWallet: Boolean = false,
    val role: AssistedWalletRole = AssistedWalletRole.NONE,
)