package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.membership.CreateTimelockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class OnChainSetUpTimelockViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val createTimelockUseCase: CreateTimelockUseCase,
    private val pushEventManager: PushEventManager,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime
    
    private val _event = MutableSharedFlow<OnChainSetUpTimelockEvent>()
    val event = _event.asSharedFlow()
    
    private val _showConfirmTimelockDateDialog = MutableStateFlow(false)
    val showConfirmTimelockDateDialog = _showConfirmTimelockDateDialog.asStateFlow()
    
    private val _showInvalidDateDialog = MutableStateFlow(false)
    val showInvalidDateDialog = _showInvalidDateDialog.asStateFlow()
    
    private val _maxTimelockYears = MutableStateFlow<Int?>(null)
    val maxTimelockYears = _maxTimelockYears.asStateFlow()
    
    private var pendingTimelockData: PendingTimelockData? = null
    
    init {
        loadMaxTimelockYears()
    }
    
    private fun loadMaxTimelockYears() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { configs ->
                    _maxTimelockYears.value = configs.maxTimelockYears
                }
            }
        }
    }

    fun onContinueClick(selectedDate: Calendar, selectedTimeZone: TimeZoneDetail, groupId: String? = null) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            
            // Check if date is in the past
            if (selectedDate.before(now)) {
                _showInvalidDateDialog.value = true
                return@launch
            }
            
            // Get max timelock years from cache
            val walletConfigs = getUserWalletConfigsSetupFromCacheUseCase(Unit).firstOrNull()?.getOrNull()
            val maxTimelockYears = walletConfigs?.maxTimelockYears ?: Int.MAX_VALUE
            
            // Calculate years difference
            val yearsDifference = selectedDate.get(Calendar.YEAR) - now.get(Calendar.YEAR)
            val monthDiff = selectedDate.get(Calendar.MONTH) - now.get(Calendar.MONTH)
            val dayDiff = selectedDate.get(Calendar.DAY_OF_MONTH) - now.get(Calendar.DAY_OF_MONTH)
            
            // Calculate actual years difference more accurately
            val actualYearsDifference = if (monthDiff < 0 || (monthDiff == 0 && dayDiff < 0)) {
                yearsDifference - 1
            } else {
                yearsDifference
            }
            
            // Check if date is more than max_timelock_years in the future
            if (actualYearsDifference > maxTimelockYears) {
                pendingTimelockData = PendingTimelockData(selectedDate, selectedTimeZone, groupId)
                _showConfirmTimelockDateDialog.value = true
                return@launch
            }
            
            // Date is valid, proceed with creating timelock
            createTimelock(selectedDate, selectedTimeZone, groupId)
        }
    }
    
    fun onConfirmTimelockDate() {
        pendingTimelockData?.let { data ->
            _showConfirmTimelockDateDialog.value = false
            createTimelock(data.selectedDate, data.selectedTimeZone, data.groupId)
            pendingTimelockData = null
        }
    }
    
    fun onDismissConfirmTimelockDateDialog() {
        _showConfirmTimelockDateDialog.value = false
        pendingTimelockData = null
    }
    
    fun onDismissInvalidDateDialog() {
        _showInvalidDateDialog.value = false
    }
    
    private fun createTimelock(selectedDate: Calendar, selectedTimeZone: TimeZoneDetail, groupId: String?) {
        viewModelScope.launch {
            val timelockValue = selectedDate.timeInMillis / 1000
            
            val result = createTimelockUseCase(
                CreateTimelockUseCase.Param(
                    groupId = groupId,
                    timelockValue = timelockValue,
                    timezone = selectedTimeZone.id,
                    plan = membershipStepManager.localMembershipPlan
                )
            )
            
            if (result.isSuccess) {
                groupId?.let {
                    pushEventManager.push(PushEvent.DraftWalletTimelockSet(it))
                }
                _event.emit(OnChainSetUpTimelockEvent.Success)
            } else {
                _event.emit(OnChainSetUpTimelockEvent.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
            }
        }
    }
    
    private data class PendingTimelockData(
        val selectedDate: Calendar,
        val selectedTimeZone: TimeZoneDetail,
        val groupId: String?
    )
}

sealed class OnChainSetUpTimelockEvent {
    data object Success : OnChainSetUpTimelockEvent()
    data class Error(val message: String) : OnChainSetUpTimelockEvent()
}