package com.nunchuk.android.main.membership.byzantine.healthcheckreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.HealthReminder
import com.nunchuk.android.usecase.membership.AddOrUpdateHealthReminderUseCase
import com.nunchuk.android.usecase.membership.DeleteHealthReminderUseCase
import com.nunchuk.android.usecase.membership.GetHealthReminderListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthCheckReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHealthReminderListUseCase: GetHealthReminderListUseCase,
    private val addOrUpdateHealthReminderUseCase: AddOrUpdateHealthReminderUseCase,
    private val deleteHealthReminderUseCase: DeleteHealthReminderUseCase,
) : ViewModel() {

    private var groupId: String? = null
    private lateinit var walletId: String

    private val _state = MutableStateFlow(HealthCheckReminderState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<HealthCheckReminderEvent>()
    val event = _event.asSharedFlow()

    fun init(groupId: String?, walletId: String) {
        this.groupId = groupId
        this.walletId = walletId

        getHealthReminderList()
    }

    private fun getHealthReminderList(silentLoading: Boolean = false) {
        viewModelScope.launch {
            if (silentLoading.not()) _event.emit(HealthCheckReminderEvent.Loading(true))
            getHealthReminderListUseCase(GetHealthReminderListUseCase.Params(groupId, walletId))
                .onSuccess { reminders ->
                    _state.update { it.copy(selectedXfps = emptyList()) }
                    _event.emit(HealthCheckReminderEvent.Loading(false))
                    _state.update {
                        it.copy(healthReminders = reminders.associateBy { it.xfp })
                    }
                }
        }
    }

    fun switchEditMode(isCurrentEditMode: Boolean): Boolean {
        if (state.value.isForceInAddMode) {
            _state.update { it.copy(isForceInAddMode = false) }
            return true
        }
        _state.update { it.copy(selectedXfps = emptyList()) }
        if (isCurrentEditMode.not() && state.value.isForceInAddMode.not()) {
            return false
        }
        if (isCurrentEditMode) return false
        return true
    }

    fun selectHealthCheck(isSelect: Boolean, xfp: String) {
        if (isSelect) {
            _state.update {
                it.copy(selectedXfps = it.selectedXfps + xfp)
            }
        } else {
            _state.update {
                it.copy(selectedXfps = it.selectedXfps - xfp)
            }
        }
    }

    fun addOrUpdateHealthReminder(frequency: String, startDate: Long, xfp: String?) {
        viewModelScope.launch {
            _event.emit(HealthCheckReminderEvent.Loading(true))
            addOrUpdateHealthReminderUseCase(
                AddOrUpdateHealthReminderUseCase.Params(
                    groupId,
                    walletId,
                    if (xfp == null) state.value.selectedXfps else listOf(xfp),
                    frequency,
                    startDate
                )
            ).onSuccess {
                getHealthReminderList(true)
                forceInAddMode(false)
                _event.emit(HealthCheckReminderEvent.Success)
            }
                .onFailure {
                    _event.emit(HealthCheckReminderEvent.Loading(false))
                    _event.emit(HealthCheckReminderEvent.Error(it.message.orUnknownError()))
                }
        }
    }

    fun deleteHealthReminder(xfp: String?) {
        viewModelScope.launch {
            _event.emit(HealthCheckReminderEvent.Loading(true))
            deleteHealthReminderUseCase(
                DeleteHealthReminderUseCase.Params(groupId, walletId, if (xfp == null) state.value.selectedXfps else listOf(xfp),)
            ).onSuccess {
                getHealthReminderList(true)
                forceInAddMode(false)
                _event.emit(HealthCheckReminderEvent.Success)
            }
                .onFailure {
                    _event.emit(HealthCheckReminderEvent.Loading(false))
                    _event.emit(HealthCheckReminderEvent.Error(it.message.orUnknownError()))
                }
        }
    }

    fun forceInAddMode(force: Boolean) {
        _state.update { it.copy(isForceInAddMode = force, switchModeCount = it.switchModeCount + 1) }
    }
}

data class HealthCheckReminderState(
    val healthReminders: Map<String, HealthReminder>? = null,
    val isForceInAddMode: Boolean = false,
    val switchModeCount: Int = 0,
    val selectedXfps: List<String> = emptyList(),
)

sealed class HealthCheckReminderEvent {
    data class Loading(val loading: Boolean) : HealthCheckReminderEvent()
    data object Success : HealthCheckReminderEvent()
    data class Error(val message: String) : HealthCheckReminderEvent()
}