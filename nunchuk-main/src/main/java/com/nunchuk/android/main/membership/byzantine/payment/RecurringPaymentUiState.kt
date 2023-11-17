package com.nunchuk.android.main.membership.byzantine.payment

import com.nunchuk.android.model.byzantine.DummyTransactionPayload

data class RecurringPaymentUiState(
    val openDummyTransactionScreen: DummyTransactionPayload? = null,
    val errorMessage: String? = null,
)