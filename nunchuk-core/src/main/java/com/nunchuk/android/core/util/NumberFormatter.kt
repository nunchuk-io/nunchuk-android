package com.nunchuk.android.core.util

import java.text.DecimalFormat

const val USD_FRACTION_DIGITS = 2

const val MIN_FRACTION_DIGITS = 2
const val MAX_FRACTION_DIGITS = 8

fun Number.formatDecimal(maxFractionDigits: Int = MAX_FRACTION_DIGITS): String {
    return DecimalFormat("##.############").apply {
        minimumFractionDigits = MIN_FRACTION_DIGITS
        maximumFractionDigits = maxFractionDigits
    }.format(this)
}
