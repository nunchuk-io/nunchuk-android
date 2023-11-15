package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class FeeRate {
    PRIORITY, STANDARD, ECONOMY
}

val String?.toFeeRate: FeeRate
    get() = FeeRate.values().firstOrNull { it.name == this } ?: FeeRate.PRIORITY