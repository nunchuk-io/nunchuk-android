package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.lifecycle.ViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class InheritanceReleaseScheduleFlowState(
    val releaseScheduleUiState: ReleaseScheduleUiState = ReleaseScheduleUiState(),
    val editingBeneficiaryEmail: String? = null,
    val pendingNewStage: ReleaseScheduleStage? = null,
)

@HiltViewModel
class InheritanceReleaseScheduleFlowViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(InheritanceReleaseScheduleFlowState())
    val state = _state.asStateFlow()

    val releaseScheduleUiState: ReleaseScheduleUiState
        get() = _state.value.releaseScheduleUiState

    val editingBeneficiaryEmail: String?
        get() = _state.value.editingBeneficiaryEmail

    val pendingNewStage: ReleaseScheduleStage?
        get() = _state.value.pendingNewStage

    fun setReleaseScheduleUiState(value: ReleaseScheduleUiState) {
        _state.update { it.copy(releaseScheduleUiState = value) }
    }

    fun setEditingBeneficiaryEmail(value: String?) {
        _state.update { it.copy(editingBeneficiaryEmail = value) }
    }

    fun setPendingNewStage(value: ReleaseScheduleStage?) {
        _state.update { it.copy(pendingNewStage = value) }
    }
}
