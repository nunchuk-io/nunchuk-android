package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.payment.RecurringPaymentResponse

internal data class RecurringPaymentListResponse(
    @SerializedName("recurring_payments")
    val recurringPayments: List<RecurringPaymentResponse> = emptyList(),
)