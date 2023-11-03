package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ListRecurringPaymentViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(ListRecurringPaymentUiState())
    val state = _state.asStateFlow()
}

data class ListRecurringPaymentUiState(
    val isLoading: Boolean = false,
)