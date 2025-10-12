package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.CreateTimelockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class OnChainSetUpTimelockViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val createTimelockUseCase: CreateTimelockUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime
    
    private val _event = MutableSharedFlow<OnChainSetUpTimelockEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClick(selectedDate: Calendar, selectedTimeZone: TimeZoneDetail, groupId: String? = null) {
        viewModelScope.launch {
            // Convert localCalendar to targetTimezoneCalendar (same logic as MiniscriptConfigTemplateNavigation.kt)
            val localCalendar = Calendar.getInstance()
            localCalendar.timeInMillis = selectedDate.timeInMillis
            
            // Create a calendar in the selected timezone and set the same date/time components
            val targetTimezoneCalendar = Calendar.getInstance(TimeZone.getTimeZone(selectedTimeZone.id))
            targetTimezoneCalendar.set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
            targetTimezoneCalendar.set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
            targetTimezoneCalendar.set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
            targetTimezoneCalendar.set(Calendar.HOUR_OF_DAY, localCalendar.get(Calendar.HOUR_OF_DAY))
            targetTimezoneCalendar.set(Calendar.MINUTE, localCalendar.get(Calendar.MINUTE))
            targetTimezoneCalendar.set(Calendar.SECOND, 0)
            targetTimezoneCalendar.set(Calendar.MILLISECOND, 0)
            
            // This gives us the UTC timestamp for the selected date/time in the selected timezone
            val timelockValue = targetTimezoneCalendar.timeInMillis / 1000
            
            val result = createTimelockUseCase(
                CreateTimelockUseCase.Param(
                    groupId = groupId,
                    timelockValue = timelockValue
                )
            )
            
            if (result.isSuccess) {
                _event.emit(OnChainSetUpTimelockEvent.Success)
            } else {
                _event.emit(OnChainSetUpTimelockEvent.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
            }
        }
    }
}

data class TimelockData(
    val selectedDate: Calendar,
    val selectedTimeZone: TimeZoneDetail
)

sealed class OnChainSetUpTimelockEvent {
    data object Success : OnChainSetUpTimelockEvent()
    data class Error(val message: String) : OnChainSetUpTimelockEvent()
}