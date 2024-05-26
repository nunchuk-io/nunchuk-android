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

    private val args = HealthCheckReminderFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private var groupId: String? = null
    private lateinit var walletId: String

    private val _state = MutableStateFlow(HealthCheckReminderState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<HealthCheckReminderEvent>()
    val event = _event.asSharedFlow()

    init {
        _state.update {
            it.copy(isEditMode = args.mode == 0, defaultMode = args.mode)
        }
    }

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
                    _event.emit(HealthCheckReminderEvent.Loading(false))
                    _state.update {
                        it.copy(healthReminders = reminders.associateBy { it.xfp })
                    }
                }
        }
    }

    fun switchEditMode(isAdd: Boolean): Boolean {
        _state.update { it.copy(selectedXfps = emptyList()) }
        if (isAdd.not()) {
            val currentMode = if (_state.value.isEditMode) 0 else 1
            if (currentMode == _state.value.defaultMode) {
                return false
            }
        }
        _state.update {
            it.copy(isEditMode = !it.isEditMode)
        }
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
                _event.emit(HealthCheckReminderEvent.Success)
            }
                .onFailure {
                    _event.emit(HealthCheckReminderEvent.Loading(false))
                    _event.emit(HealthCheckReminderEvent.Error(it.message.orUnknownError()))
                }
        }
    }
}

data class HealthCheckReminderState(
    val healthReminders: Map<String, HealthReminder>? = null,
    val isEditMode: Boolean = false, // 0: edit, 1: add,
    val defaultMode: Int = 0,
    val selectedXfps: List<String> = emptyList(),
)

sealed class HealthCheckReminderEvent {
    data class Loading(val loading: Boolean) : HealthCheckReminderEvent()
    data object Success : HealthCheckReminderEvent()
    data class Error(val message: String) : HealthCheckReminderEvent()
}