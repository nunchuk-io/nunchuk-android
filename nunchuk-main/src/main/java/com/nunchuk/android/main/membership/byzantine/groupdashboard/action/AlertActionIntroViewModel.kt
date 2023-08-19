package com.nunchuk.android.main.membership.byzantine.groupdashboard.action

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionPayloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertActionIntroViewModel @Inject constructor(
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
    private val getGroupDummyTransactionPayloadUseCase: GetGroupDummyTransactionPayloadUseCase,
    saveStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = AlertActionIntroFragmentArgs.fromSavedStateHandle(saveStateHandle)
    private val _event = MutableSharedFlow<AlertActionIntroEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AlertActionIntroUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupDummyTransactionPayloadUseCase(
                GetGroupDummyTransactionPayloadUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.alert.payload.dummyTransactionId
                )
            ).onSuccess {
                _state.update { state ->
                    state.copy(dummyTransaction = it)
                }
            }
        }
    }

    fun deleteDummyTransaction() {
        viewModelScope.launch {
            _event.emit(AlertActionIntroEvent.Loading(true))
            deleteGroupDummyTransactionUseCase(
                DeleteGroupDummyTransactionUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.alert.payload.dummyTransactionId
                )
            ).onSuccess {
                _event.emit(AlertActionIntroEvent.DeleteDummyTransactionSuccess)
            }
            _event.emit(AlertActionIntroEvent.Loading(false))
        }
    }
}

sealed class AlertActionIntroEvent {
    object DeleteDummyTransactionSuccess : AlertActionIntroEvent()
    data class Loading(val isLoading: Boolean) : AlertActionIntroEvent()
}

data class AlertActionIntroUiState(val dummyTransaction: DummyTransactionPayload? = null)