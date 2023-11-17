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

    init {
        viewModelScope.launch {
            getRecurringPaymentsUseCase(
                GetRecurringPaymentsUseCase.Params(
                    walletId = walletId,
                    groupId = groupId,
                )
            ).onSuccess {
                _state.update {
                    it.copy(
                        payments = it.payments,
                    )
                }
            }
        }
    }
}

data class ListRecurringPaymentUiState(
    val payments: List<RecurringPayment> = emptyList(),
)