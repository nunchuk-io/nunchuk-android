package com.nunchuk.android.utils

import com.nunchuk.android.model.Transaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

const val MIN_FRACTION_DIGITS = 2
const val MAX_FRACTION_DIGITS = 8

fun Long.formatDate(): String = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm aaa", Locale.US).format(Date(this * 1000))

fun Number.formatDecimal(): String {
    return DecimalFormat("##.############").apply {
        minimumFractionDigits = MIN_FRACTION_DIGITS
        maximumFractionDigits = MAX_FRACTION_DIGITS
    }.format(this)
}

fun Transaction.getFormatDate(): String = if (blockTime <= 0) "" else (blockTime).formatDate()