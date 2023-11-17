package com.nunchuk.android.main.membership.byzantine.groupdashboard.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentRequestReviewViewModel @Inject constructor(
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
    saveStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = RecurringPaymentRequestReviewFragmentArgs.fromSavedStateHandle(saveStateHandle)
    private val _event = MutableSharedFlow<RecurringPaymentRequestReviewEvent>()
    val event = _event.asSharedFlow()
    fun deleteDummyTransaction() {
        viewModelScope.launch {
            deleteGroupDummyTransactionUseCase(
                DeleteGroupDummyTransactionUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.dummyTransactionId
                )
            ).onSuccess {
                _event.emit(RecurringPaymentRequestReviewEvent.DeleteDummyTransaction)
            }.onFailure {
                _event.emit(RecurringPaymentRequestReviewEvent.ShowError(it.message.orEmpty()))
            }
        }
    }
}

sealed class RecurringPaymentRequestReviewEvent {
    data object DeleteDummyTransaction : RecurringPaymentRequestReviewEvent()
    data class ShowError(val message: String) : RecurringPaymentRequestReviewEvent()
}