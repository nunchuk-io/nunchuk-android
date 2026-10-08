package com.nunchuk.android.main.membership.byzantine.payment

import androidx.annotation.StringRes
import com.nunchuk.android.main.R
import com.nunchuk.android.model.payment.PaymentFrequency

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
/** Frequency unit without the "Every" prefix, e.g. "day" or "three months". */
@StringRes
fun PaymentFrequency.toUnitResId(): Int {
    return when (this) {
        PaymentFrequency.DAILY -> R.string.nc_membership_frequency_unit_day
        PaymentFrequency.WEEKLY -> R.string.nc_membership_frequency_unit_week
        PaymentFrequency.MONTHLY -> R.string.nc_membership_frequency_unit_month
        PaymentFrequency.THREE_MONTHLY -> R.string.nc_membership_frequency_unit_three_months
        PaymentFrequency.SIX_MONTHLY -> R.string.nc_membership_frequency_unit_six_months
        PaymentFrequency.YEARLY -> R.string.nc_membership_frequency_unit_year
    }
}
