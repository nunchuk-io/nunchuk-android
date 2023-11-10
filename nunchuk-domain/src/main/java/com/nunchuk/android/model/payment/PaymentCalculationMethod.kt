package com.nunchuk.android.model.payment

import androidx.annotation.Keep

@Keep
enum class PaymentCalculationMethod {
    RUNNING_AVERAGE, JUST_IN_TIME
}