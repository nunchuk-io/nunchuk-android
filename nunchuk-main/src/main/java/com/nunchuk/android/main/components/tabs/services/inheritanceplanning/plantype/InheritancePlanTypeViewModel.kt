package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.plantype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritancePlanTypeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(InheritancePlanTypeUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InheritancePlanTypeEvent>()
    val event = _event.asSharedFlow()

    fun onPlanTypeSelected(planType: InheritancePlanType) {
        viewModelScope.launch {
            _state.emit(_state.value.copy(selectedPlanType = planType))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(InheritancePlanTypeEvent.OnContinueClicked(_state.value.selectedPlanType))
        }
    }
}
