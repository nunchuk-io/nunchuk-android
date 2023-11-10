package com.nunchuk.android.main.membership.byzantine.payment.frequent

import androidx.annotation.StringRes
import com.nunchuk.android.main.R

enum class PaymentFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    THREE_MONTHLY,
    SIX_MONTHLY,
    YEARLY,
}

@StringRes
fun PaymentFrequency.toResId(): Int {
   return when (this) {
        PaymentFrequency.DAILY -> R.string.nc_every_day
        PaymentFrequency.WEEKLY -> R.string.nc_every_week
        PaymentFrequency.MONTHLY -> R.string.nc_every_month
        PaymentFrequency.THREE_MONTHLY -> R.string.nc_every_three_months
        PaymentFrequency.SIX_MONTHLY -> R.string.nc_every_six_months
        PaymentFrequency.YEARLY -> R.string.nc_every_year
    }
}