package com.nunchuk.android.main.membership.byzantine.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
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
    private val getGroupUseCase: GetGroupUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
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
            _state.update { it.copy(isLoading = true) }
            isViewPendingGroupUseCase(args.groupId).onSuccess { isView ->
                val group =
                    getGroupUseCase(GetGroupUseCase.Params(args.groupId)).map { it.getOrNull() }.firstOrNull()
                val email = accountManager.getAccount().email
                val walletType = syncDraftWalletUseCase(args.groupId).getOrNull()?.walletType
                _state.update {
                    it.copy(
                        isViewPendingWallet = isView,
                        masterName = group?.getMasterName().orEmpty(),
                        role = group?.members?.find { member -> member.emailOrUsername == email }?.role.toRole,
                        walletType = walletType,
                    )
                }
                if (!isView) {
                    setViewPendingGroupUseCase(args.groupId)
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun markHandleViewPendingWallet() {
        _state.update { it.copy(isViewPendingWallet = false) }
    }

    fun isOnChainWallet() =
        state.value.walletType == WalletType.MINISCRIPT

    fun getRole() = state.value.role
}

data class GroupPendingIntroUiState(
    val isLoading: Boolean = false,
    val masterName: String = "",
    val isViewPendingWallet: Boolean = false,
    val role: AssistedWalletRole = AssistedWalletRole.NONE,
    val walletType: WalletType? = null
)