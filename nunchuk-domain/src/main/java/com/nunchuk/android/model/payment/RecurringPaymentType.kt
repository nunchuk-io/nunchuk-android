package com.nunchuk.android.model.payment

import androidx.annotation.Keep

@Keep
enum class RecurringPaymentType {
    FIXED_AMOUNT, PERCENTAGE
}

val String?.toRecurringPaymentType: RecurringPaymentType
    get() = RecurringPaymentType.values().find { it.name == this } ?: RecurringPaymentType.FIXED_AMOUNT