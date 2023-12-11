package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.payment.RecurringPaymentDto

internal data class RecurringPaymentListResponse(
    @SerializedName("recurring_payments")
    val recurringPayments: List<RecurringPaymentDto> = emptyList(),
)

internal data class RecurringPaymentResponse(
    @SerializedName("recurring_payment")
    val recurringPayment: RecurringPaymentDto? = null
)