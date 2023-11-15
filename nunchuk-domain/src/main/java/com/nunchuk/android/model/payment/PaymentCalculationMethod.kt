package com.nunchuk.android.model.payment

import androidx.annotation.Keep

@Keep
enum class PaymentCalculationMethod {
    RUNNING_AVERAGE, JUST_IN_TIME
}

val String?.toPaymentCalculationMethod: PaymentCalculationMethod?
    get() = PaymentCalculationMethod.values().find { it.name == this }