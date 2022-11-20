package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class SpendingCurrencyUnit {
    USD, BTC, SAT
}

@Keep
enum class SpendingTimeUnit {
    DAILY, WEEKLY, MONTHLY, YEARLY
}