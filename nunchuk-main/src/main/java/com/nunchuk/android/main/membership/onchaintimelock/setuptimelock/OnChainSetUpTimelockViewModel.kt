package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.TimelockExtra
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.GetUserWalletConfigsSetupFromCacheUseCase
import com.nunchuk.android.usecase.membership.ConvertTimelockUseCase
import com.nunchuk.android.usecase.membership.CreateTimelockUseCase
import com.nunchuk.android.usecase.replace.ReplaceTimelockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val UNIX_Y2K38_THRESHOLD = 2147483648L // Jan 19, 2038 03:14:08 UTC

@HiltViewModel
class OnChainSetUpTimelockViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val createTimelockUseCase: CreateTimelockUseCase,
    private val replaceTimelockUseCase: ReplaceTimelockUseCase,
    private val pushEventManager: PushEventManager,
    private val getUserWalletConfigsSetupFromCacheUseCase: GetUserWalletConfigsSetupFromCacheUseCase,
    private val convertTimelockUseCase: ConvertTimelockUseCase,
) : ViewModel() {

    val remainTime = membershipStepManager.remainingTime

    private val _event = MutableSharedFlow<OnChainSetUpTimelockEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(OnChainSetUpTimelockState())
    val state = _state.asStateFlow()

    init {
        loadMaxTimelockYears()
    }

    fun initFromTimelockExtra(timelockExtra: TimelockExtra?) {
        if (timelockExtra != null && timelockExtra.based == TimelockBased.HEIGHT_LOCK) {
            _state.update {
                it.copy(
                    isBlockBased = true,
                    blockHeight = timelockExtra.blockHeight
                )
            }
        }
    }

    private fun loadMaxTimelockYears() {
        viewModelScope.launch {
            getUserWalletConfigsSetupFromCacheUseCase(Unit).collect { result ->
                result.getOrNull()?.let { configs ->
                    _state.update { it.copy(maxTimelockYears = configs.maxTimelockYears) }
                }
            }
        }
    }

    fun onContinueClick(
        selectedDate: Calendar,
        selectedTimeZone: TimeZoneDetail,
        groupId: String? = null,
        isReplaceKeyFlow: Boolean = false,
        walletId: String? = null
    ) {
        viewModelScope.launch {
            val now = Calendar.getInstance()

            // Check if date is in the past
            if (selectedDate.before(now)) {
                _state.update { it.copy(showInvalidDateDialog = true) }
                return@launch
            }

            // Get max timelock years from cache
            val walletConfigs =
                getUserWalletConfigsSetupFromCacheUseCase(Unit).firstOrNull()?.getOrNull()
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
                _state.update {
                    it.copy(
                        pendingTimelockData = PendingTimelockData(
                            selectedDate = selectedDate,
                            selectedTimeZone = selectedTimeZone,
                            groupId = groupId,
                            isReplaceKeyFlow = isReplaceKeyFlow,
                            walletId = walletId
                        ),
                        showConfirmTimelockDateDialog = true
                    )
                }
                return@launch
            }

            // Date is valid, proceed with creating timelock
            createTimelock(selectedDate, selectedTimeZone, groupId, isReplaceKeyFlow, walletId)
        }
    }

    fun onConfirmBlockBasedTimelock() {
        viewModelScope.launch {
            _state.value.pendingTimelockData?.let { data ->
                _state.update {
                    it.copy(
                        showBlockBasedTimelockDialog = false,
                        isLoading = true
                    )
                }

                val timelockValue = data.selectedDate.timeInMillis / 1000

                // Call ConvertTimelockUseCase to get the block height
                val result = convertTimelockUseCase(
                    ConvertTimelockUseCase.Param(
                        value = timelockValue,
                        timezone = data.selectedTimeZone.id,
                        based = TimelockBased.TIME_LOCK,
                        blockHeight = 0L
                    )
                )

                result.onSuccess { convertedTimelock ->
                    _state.update {
                        it.copy(
                            isBlockBased = true,
                            blockHeight = convertedTimelock.blockHeight,
                            isLoading = false
                        )
                    }
                }.onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            pendingTimelockData = null
                        )
                    }
                    _event.emit(OnChainSetUpTimelockEvent.Error(e.message ?: "Failed to convert timelock"))
                }
            }
        }
    }

    fun onDismissBlockBasedTimelockDialog() {
        _state.update {
            it.copy(
                showBlockBasedTimelockDialog = false,
                pendingTimelockData = null
            )
        }
    }

    fun onDateChanged(selectedDate: Calendar, selectedTimeZone: TimeZoneDetail) {
        val timelockValue = selectedDate.timeInMillis / 1000
        if (timelockValue >= UNIX_Y2K38_THRESHOLD) {
            if (!_state.value.isBlockBased) {
                // Date exceeds Y2K38 threshold, show block-based timelock dialog
                _state.update {
                    it.copy(
                        pendingTimelockData = PendingTimelockData(
                            selectedDate = selectedDate,
                            selectedTimeZone = selectedTimeZone,
                            groupId = null
                        ),
                        showBlockBasedTimelockDialog = true
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        pendingTimelockData = PendingTimelockData(
                            selectedDate = selectedDate,
                            selectedTimeZone = selectedTimeZone,
                            groupId = null
                        ),
                        showBlockBasedTimelockDialog = false
                    )
                }
                onConfirmBlockBasedTimelock()
            }
        } else if (_state.value.isBlockBased) {
            // Date is changed to before Y2K38 threshold, reset block-based state
            _state.update {
                it.copy(
                    isBlockBased = false,
                    blockHeight = null,
                    pendingTimelockData = null
                )
            }
        }
    }

    fun onConfirmTimelockDate() {
        _state.value.pendingTimelockData?.let { data ->
            _state.update {
                it.copy(
                    showConfirmTimelockDateDialog = false,
                    pendingTimelockData = null
                )
            }
            createTimelock(
                selectedDate = data.selectedDate,
                selectedTimeZone = data.selectedTimeZone,
                groupId = data.groupId,
                isReplaceKeyFlow = data.isReplaceKeyFlow,
                walletId = data.walletId
            )
        }
    }

    fun onDismissConfirmTimelockDateDialog() {
        _state.update {
            it.copy(
                showConfirmTimelockDateDialog = false,
                pendingTimelockData = null
            )
        }
    }

    fun onDismissInvalidDateDialog() {
        _state.update { it.copy(showInvalidDateDialog = false) }
    }

    private fun createTimelock(
        selectedDate: Calendar, selectedTimeZone: TimeZoneDetail, groupId: String?,
        isReplaceKeyFlow: Boolean = false,
        walletId: String? = null
    ) {
        viewModelScope.launch {
            val timelockValue = selectedDate.timeInMillis / 1000
            val isBlockBased = _state.value.isBlockBased
            val blockHeight = _state.value.blockHeight

            if (isReplaceKeyFlow && walletId != null) {
                // Use ReplaceTimelockUseCase for replace key flow
                val result = replaceTimelockUseCase(
                    ReplaceTimelockUseCase.Param(
                        groupId = groupId,
                        walletId = walletId,
                        timelockValue = timelockValue,
                        timezone = selectedTimeZone.id,
                        based = if (isBlockBased) TimelockBased.HEIGHT_LOCK else TimelockBased.TIME_LOCK,
                        blockHeight = blockHeight
                    )
                )

                if (result.isSuccess) {
                    pushEventManager.push(PushEvent.ReplaceKeyChange(walletId))
                    _event.emit(OnChainSetUpTimelockEvent.Success)
                } else {
                    _event.emit(
                        OnChainSetUpTimelockEvent.Error(
                            result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            } else {
                // Use CreateTimelockUseCase for regular flow
                val result = createTimelockUseCase(
                    CreateTimelockUseCase.Param(
                        groupId = groupId,
                        timelockValue = timelockValue,
                        timezone = selectedTimeZone.id,
                        plan = membershipStepManager.localMembershipPlan,
                        based = if (isBlockBased) TimelockBased.HEIGHT_LOCK else TimelockBased.TIME_LOCK,
                        blockHeight = blockHeight
                    )
                )

                if (result.isSuccess) {
                    pushEventManager.push(PushEvent.DraftWalletTimelockSet(groupId.orEmpty()))
                    _event.emit(OnChainSetUpTimelockEvent.Success)
                } else {
                    _event.emit(
                        OnChainSetUpTimelockEvent.Error(
                            result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }
}

data class PendingTimelockData(
    val selectedDate: Calendar,
    val selectedTimeZone: TimeZoneDetail,
    val groupId: String?,
    val isReplaceKeyFlow: Boolean = false,
    val walletId: String? = null
)

data class OnChainSetUpTimelockState(
    val showConfirmTimelockDateDialog: Boolean = false,
    val showInvalidDateDialog: Boolean = false,
    val showBlockBasedTimelockDialog: Boolean = false,
    val maxTimelockYears: Int? = null,
    val pendingTimelockData: PendingTimelockData? = null,
    val isBlockBased: Boolean = false,
    val blockHeight: Long? = null,
    val isLoading: Boolean = false
)

sealed class OnChainSetUpTimelockEvent {
    data object Success : OnChainSetUpTimelockEvent()
    data class Error(val message: String) : OnChainSetUpTimelockEvent()
}