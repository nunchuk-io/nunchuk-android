package com.nunchuk.android.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.MIN_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.getBTCAmountWithoutSat
import com.nunchuk.android.model.payment.RecurringPayment
import com.nunchuk.android.model.payment.RecurringPaymentType
import java.util.Calendar

@Composable
fun Long?.healthCheckTimeColor(): Color {
    if (this == null) return colorResource(id = R.color.nc_red_tint_color)
    val last6Month = Calendar.getInstance().apply {
        add(Calendar.MONTH, -6)
    }.timeInMillis
    val lastYear = Calendar.getInstance().apply {
        add(Calendar.YEAR, -1)
    }.timeInMillis

    return when {
        this >= last6Month -> colorResource(id = R.color.nc_green_color)
        this in lastYear until last6Month -> colorResource(id = R.color.nc_beeswax_tint)
        else -> colorResource(id = R.color.nc_red_tint_color)
    }
}

fun Long?.healthCheckLabel(context: Context): String {
    if (this == null || this == 0L) return context.getString(R.string.nc_not_checked_yet)
    val last6Month = Calendar.getInstance().apply {
        add(Calendar.MONTH, -6)
    }.timeInMillis
    val lastYear = Calendar.getInstance().apply {
        add(Calendar.YEAR, -1)
    }.timeInMillis

    return when {
        this >= last6Month -> context.getString(R.string.nc_last_checked_less_than_6_months_ago)
        this in lastYear until last6Month -> context.getString(R.string.nc_last_checked_more_than_6_months_ago)
        else -> context.getString(R.string.nc_last_checked_more_than_1_year_ago)
    }
}

val RecurringPayment.formatAmount: String
    get() = if (paymentType == RecurringPaymentType.PERCENTAGE) {
        amount.formatDecimal(
            minFractionDigits = 0,
            maxFractionDigits = 4
        )
    } else if (currency == "BTC") {
        amount.getBTCAmountWithoutSat()
    } else if (currency == "USD") {
        amount.formatDecimal(
            minFractionDigits = MIN_FRACTION_DIGITS,
            maxFractionDigits = MIN_FRACTION_DIGITS
        )
    } else {
        amount.formatDecimal(
            minFractionDigits = 0,
            maxFractionDigits = MIN_FRACTION_DIGITS
        )
    }