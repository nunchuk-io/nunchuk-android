package com.nunchuk.android.main.membership.byzantine.payment

import com.nunchuk.android.model.byzantine.DummyTransactionPayload

data class RecurringPaymentUiState(
    val hasServerKey: Boolean = false,
    val openDummyTransactionScreen: DummyTransactionPayload? = null,
    val openBsmsScreen: String? = null,
    val errorMessage: String? = null,
    val isMyWallet: Boolean = false,
)