package com.nunchuk.android.main.membership.byzantine.groupdashboard.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.byzantine.ParseRecurringPaymentPayloadUseCase
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentRequestViewModel @Inject constructor(
    private val getGroupDummyTransactionPayloadUseCase: GetGroupDummyTransactionPayloadUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val parseRecurringPaymentPayloadUseCase: ParseRecurringPaymentPayloadUseCase,
) : ViewModel() {
    private val args = RecurringPaymentRequestFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<RecurringPaymentRequestEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RecurringPaymentRequestUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getGroupDummyTransactionPayloadUseCase(
                GetGroupDummyTransactionPayloadUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.dummyTransactionId
                )
            ).onSuccess { payload ->
                parseRecurringPaymentPayloadUseCase(payload)
                    .onSuccess { recurringPayment ->
                        _state.update { state ->
                            state.copy(
                                recurringPayment = recurringPayment,
                                pendingSignatures = payload.pendingSignatures
                            )
                        }
                        getGroup(payload.requestByUserId)
                    }
            }
        }
    }

    private suspend fun getGroup(requestByUserId: String) {
        getGroupUseCase(
            GetGroupUseCase.Params(
                args.groupId
            )
        ).map { it.getOrElse { null } }
            .distinctUntilChanged()
            .collect { group ->
                val requester = group?.members.orEmpty().find { it.user?.id == requestByUserId }
                _state.update { it.copy(requester = requester) }
            }
    }
}

sealed class RecurringPaymentRequestEvent

data class RecurringPaymentRequestUiState(
    val recurringPayment: RecurringPayment? = null,
    val pendingSignatures: Int = 0,
    val requester: ByzantineMember? = null,
)