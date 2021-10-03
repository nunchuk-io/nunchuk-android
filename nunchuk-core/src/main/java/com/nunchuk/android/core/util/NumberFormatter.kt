package com.nunchuk.android.core.util

import java.text.DecimalFormat

const val MIN_FRACTION_DIGITS = 2
const val MAX_FRACTION_DIGITS = 8

fun Number.formatDecimal(): String {
    return DecimalFormat("##.############").apply {
        minimumFractionDigits = MIN_FRACTION_DIGITS
        maximumFractionDigits = MAX_FRACTION_DIGITS
    }.format(this)
}
