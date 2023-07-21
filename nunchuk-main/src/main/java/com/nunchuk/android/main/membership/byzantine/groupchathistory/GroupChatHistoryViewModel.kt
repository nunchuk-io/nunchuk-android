package com.nunchuk.android.main.membership.byzantine.groupchathistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardFragmentArgs
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.usecase.membership.CreateOrUpdateGroupChatUseCase
import com.nunchuk.android.usecase.membership.GetHistoryPeriodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistoryPeriodUseCase: GetHistoryPeriodUseCase,
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase
) : ViewModel() {

    private val args = GroupChatHistoryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupChatHistoryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(GroupChatHistoryState())
    val state = _state.asStateFlow()

    init {
       setHistoryPeriod(args.historyPeriodId)
        viewModelScope.launch {
            _event.emit(GroupChatHistoryEvent.Loading(true))
            val result = getHistoryPeriodUseCase(Unit)
            _event.emit(GroupChatHistoryEvent.Loading(false))
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    historyPeriods = result.getOrNull() ?: emptyList()
                )
            } else {
                _event.emit(GroupChatHistoryEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun saveHistoryPeriod() {
        viewModelScope.launch {
            val historyPeriod = _state.value.historyPeriods.find { it.id == _state.value.selectedHistoryPeriodId} ?: return@launch
            _event.emit(GroupChatHistoryEvent.Loading(true))
            val result = createOrUpdateGroupChatUseCase(
                CreateOrUpdateGroupChatUseCase.Param(args.groupId, _state.value.selectedHistoryPeriodId)
            )
            _event.emit(GroupChatHistoryEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(GroupChatHistoryEvent.UpdateGroupChatSuccess(historyPeriod))
            } else {
                _event.emit(GroupChatHistoryEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun setHistoryPeriod(periodId: String) {
        _state.value = _state.value.copy(
            selectedHistoryPeriodId = periodId
        )
    }
}