package com.nunchuk.android.main.membership.byzantine.groupchathistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.byzantine.SetRoomRetentionUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.byzantine.CreateOrUpdateGroupChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class GroupChatHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setRoomRetentionUseCase: SetRoomRetentionUseCase,
    private val createOrUpdateGroupChatUseCase: CreateOrUpdateGroupChatUseCase
) : ViewModel() {

    private val args = GroupChatHistoryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupChatHistoryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(GroupChatHistoryState())
    val state = _state.asStateFlow()

    init {
        setHistoryPeriod(args.historyPeriodId)
        _state.value = _state.value.copy(
            historyPeriods = args.periods.toList()
        )
    }

    fun saveHistoryPeriod() {
        viewModelScope.launch {
            val historyPeriod =
                _state.value.historyPeriods.find { it.id == _state.value.selectedHistoryPeriodId }
                    ?: return@launch
            setRoomRetentionUseCase(
                SetRoomRetentionUseCase.Param(
                    args.roomId,
                    historyPeriod.durationInMillis.milliseconds
                )
            )
            _event.emit(GroupChatHistoryEvent.Loading(true))
            val result = createOrUpdateGroupChatUseCase(
                CreateOrUpdateGroupChatUseCase.Param(
                    groupId = args.groupId,
                    roomId = args.roomId,
                    historyPeriodId = _state.value.selectedHistoryPeriodId
                )
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
        _state.update {
            it.copy(
                selectedHistoryPeriodId = periodId
            )
        }
    }
}