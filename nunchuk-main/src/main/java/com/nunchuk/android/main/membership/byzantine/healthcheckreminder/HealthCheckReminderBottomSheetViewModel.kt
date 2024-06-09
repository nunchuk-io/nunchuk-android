package com.nunchuk.android.main.membership.byzantine.healthcheckreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nunchuk.android.main.membership.byzantine.groupchathistory.GroupChatHistoryEvent
import com.nunchuk.android.model.HealthReminderFrequency
import com.nunchuk.android.model.toHealthReminderFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HealthCheckReminderBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = HealthCheckReminderBottomSheetArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<GroupChatHistoryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(HealthCheckReminderBottomSheetState(startDate = Calendar.getInstance().timeInMillis))
    val state = _state.asStateFlow()

    init {
        args.selectHealthReminder?.let { reminder ->
            _state.update {
                it.copy(
                    selectedReminder = reminder.frequency.toHealthReminderFrequency(),
                    startDate = reminder.startDateMillis
                )
            }
        }
    }

    fun selectReminder(reminder: HealthReminderFrequency) {
        _state.update {
            it.copy(selectedReminder = reminder)
        }
    }

    fun getHealthReminderFrequency(): HealthReminderFrequency {
        return _state.value.selectedReminder
    }

    fun setStartDate(startDate: Long) {
        _state.update {
            it.copy(startDate = startDate)
        }
    }

    fun getStartDate(): Long {
        return _state.value.startDate
    }
}

data class HealthCheckReminderBottomSheetState(
    val selectedReminder: HealthReminderFrequency = HealthReminderFrequency.NONE,
    val startDate: Long = 0,
)