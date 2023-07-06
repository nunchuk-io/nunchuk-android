package com.nunchuk.android.main.membership.key.desktop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.exception.RequestAddKeyCancelException
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaitingDesktopKeyViewModel @Inject constructor(
    private val pushEventManager: PushEventManager,
    private val checkRequestAddDesktopKeyStatusUseCase: CheckRequestAddDesktopKeyStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: WaitingDesktopKeyFragmentArgs =
        WaitingDesktopKeyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _state = MutableStateFlow(WaitingDesktopKeyUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            pushEventManager.event.collect {
                if (it is PushEvent.AddDesktopKeyCompleted) {
                    checkRequestStatus()
                }
            }
        }
    }

    fun checkRequestStatus() {
        viewModelScope.launch {
            checkRequestAddDesktopKeyStatusUseCase(
                CheckRequestAddDesktopKeyStatusUseCase.Param(
                    membershipStepManager.plan,
                    args.requestId
                )
            ).onSuccess {
                _state.update { state -> state.copy(isCompleted = it) }
            }.onFailure {
                _state.update { state -> state.copy(isCompleted = false, requestCancel = it is RequestAddKeyCancelException) }
            }
        }
    }

    fun markHandleAddKeyResult() {
        _state.update { state -> state.copy(isCompleted = null) }
    }
}

data class WaitingDesktopKeyUiState(val isCompleted: Boolean? = null, val error: String? = null, val requestCancel: Boolean = false)