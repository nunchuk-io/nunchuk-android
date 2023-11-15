package com.nunchuk.android.main.membership.byzantine.groupdashboard.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringPaymentRequestReviewViewModel @Inject constructor(
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
    saveStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = RecurringPaymentRequestReviewFragmentArgs.fromSavedStateHandle(saveStateHandle)
    fun deleteDummyTransaction() {
        viewModelScope.launch {
            deleteGroupDummyTransactionUseCase(
                DeleteGroupDummyTransactionUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.dummyTransactionId
                )
            ).onSuccess {

            }.onFailure {

            }
        }
    }
}