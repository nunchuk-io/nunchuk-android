package com.nunchuk.android.model.byzantine

import com.nunchuk.android.model.SpendingTimeUnit

data class InputSpendingPolicy(
    val limit: String,
    val timeUnit: SpendingTimeUnit,
    val currencyUnit: String,
)