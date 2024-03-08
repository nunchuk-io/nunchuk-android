package com.nunchuk.android.main.membership.byzantine.payment.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.premier.DeleteRecurringPaymentUseCase
import com.nunchuk.android.usecase.premier.GetRecurringPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentDetailViewModel @Inject constructor(
    private val getRecurringPaymentUseCase: GetRecurringPaymentUseCase,
    private val deleteRecurringPaymentUseCase: DeleteRecurringPaymentUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val recurringPaymentId =
        requireNotNull(savedStateHandle.get<String>("recurringPaymentId"))
    private val groupId = requireNotNull(savedStateHandle.get<String>("groupId"))
    private val walletId = requireNotNull(savedStateHandle.get<String>("walletId"))

    private val _state = MutableStateFlow(RecurringPaymentDetailUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }
            getRecurringPaymentUseCase(
                GetRecurringPaymentUseCase.Params(
                    groupId = groupId,
                    walletId = walletId,
                    id = recurringPaymentId,
                )
            ).onSuccess { payment ->
                _state.update {
                    it.copy(recurringPayment = payment)
                }
            }.onFailure {
                _state.update {
                    it.copy(errorMessage = it.errorMessage)
                }
            }
            _state.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun onCancelPayment() {
        viewModelScope.launch {
            deleteRecurringPaymentUseCase(
                DeleteRecurringPaymentUseCase.Params(
                    groupId = groupId,
                    walletId = walletId,
                    id = recurringPaymentId,
                )
            ).onSuccess { payload ->
                _state.update {
                    it.copy(openDummyTransactionPayload = payload)
                }
            }.onFailure {
                _state.update {
                    it.copy(errorMessage = it.errorMessage)
                }
            }
        }
    }

    fun onOpenDummyTransactionScreenComplete() {
        _state.update {
            it.copy(openDummyTransactionPayload = null)
        }
    }

    suspend fun getGroupConfig(): GroupWalletType? {
        return getGroupUseCase(GetGroupUseCase.Params(groupId))
            .map { it.getOrNull() }
            .firstOrNull()?.walletConfig?.toGroupWalletType()
    }
}

data class RecurringPaymentDetailUiState(
    val recurringPayment: RecurringPayment? = null,
    val openDummyTransactionPayload: DummyTransactionPayload? = null,
    val errorMessage: String? = null,
    val isLoading : Boolean = false,
)