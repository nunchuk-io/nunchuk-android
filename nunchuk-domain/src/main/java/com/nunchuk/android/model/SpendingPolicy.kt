package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpendingPolicy(
    val limit: Long,
    val timeUnit: SpendingTimeUnit,
    val currencyUnit: SpendingCurrencyUnit,
) : Parcelable