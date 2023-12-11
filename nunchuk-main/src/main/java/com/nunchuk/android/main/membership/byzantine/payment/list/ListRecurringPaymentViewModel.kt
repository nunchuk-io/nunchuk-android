package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.usecase.premier.GetRecurringPaymentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListRecurringPaymentViewModel @Inject constructor(
    private val getRecurringPaymentsUseCase: GetRecurringPaymentsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val walletId = checkNotNull(savedStateHandle.get<String>("walletId"))
    private val groupId = checkNotNull(savedStateHandle.get<String>("groupId"))

    private val _state = MutableStateFlow(ListRecurringPaymentUiState())
    val state = _state.asStateFlow()
    private val _payments: MutableList<RecurringPayment> = mutableListOf()

    init {
        viewModelScope.launch {
            getRecurringPaymentsUseCase(
                GetRecurringPaymentsUseCase.Params(
                    walletId = walletId,
                    groupId = groupId,
                )
            ).onSuccess { payments ->
                _payments.addAll(payments.toMutableList())
                _state.update { it.copy(payments = _payments) }
            }
        }
    }

    fun sort(sortBy: SortBy) {
        val newPayments = when (sortBy) {
            SortBy.OLDEST -> _payments.sortedByDescending { it.startDate }
            SortBy.NEWEST -> _payments.sortedBy { it.startDate }
            SortBy.AZ -> _payments.sortedBy { it.name }
            SortBy.ZA -> _payments.sortedByDescending { it.name }
            SortBy.NONE -> _payments

        }
        _state.update { state -> state.copy(payments = newPayments, sortBy = sortBy) }

    }
}

data class ListRecurringPaymentUiState(
    val payments: List<RecurringPayment> = emptyList(),
    val sortBy: SortBy = SortBy.NONE,
)