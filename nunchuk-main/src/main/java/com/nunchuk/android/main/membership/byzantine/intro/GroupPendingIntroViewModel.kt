package com.nunchuk.android.main.membership.byzantine.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.byzantine.GetGroupBriefByIdFlowUseCase
import com.nunchuk.android.usecase.user.IsViewPendingGroupUseCase
import com.nunchuk.android.usecase.user.SetViewPendingGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupPendingIntroViewModel @Inject constructor(
    private val getGroupBriefByIdFlowUseCase: GetGroupBriefByIdFlowUseCase,
    private val isViewPendingGroupUseCase: IsViewPendingGroupUseCase,
    private val setViewPendingGroupUseCase: SetViewPendingGroupUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = GroupPendingIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(GroupPendingIntroUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            isViewPendingGroupUseCase(args.groupId).onSuccess { isView ->
                _state.update { it.copy(isViewPendingWallet = isView) }
                if (!isView) {
                    setViewPendingGroupUseCase(args.groupId)
                }
            }
        }
        viewModelScope.launch {
            getGroupBriefByIdFlowUseCase(args.groupId)
                .map { it.getOrNull() }
                .filterNotNull()
                .collect { groupBrief ->
                    _state.update { it.copy(masterName = groupBrief.getMasterName()) }
                }
        }
    }

    fun markHandleViewPendingWallet() {
        _state.update { it.copy(isViewPendingWallet = false) }
    }
}

data class GroupPendingIntroUiState(val masterName: String = "", val isViewPendingWallet: Boolean = false)