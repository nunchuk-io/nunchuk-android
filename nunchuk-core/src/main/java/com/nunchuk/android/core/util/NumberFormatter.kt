package com.nunchuk.android.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

const val USD_FRACTION_DIGITS = 2

const val MIN_FRACTION_DIGITS = 2
const val MAX_FRACTION_DIGITS = 8

fun Number.formatDecimal(maxFractionDigits: Int = MAX_FRACTION_DIGITS): String {
    return DecimalFormat("##.############").apply {
        minimumFractionDigits = MIN_FRACTION_DIGITS
        maximumFractionDigits = maxFractionDigits
    }.format(this)
}

fun Number.formatCurrencyDecimal(maxFractionDigits: Int = MAX_FRACTION_DIGITS, locale: Locale = Locale.US): String {
    return NumberFormat.getCurrencyInstance(locale).apply {
        minimumFractionDigits = MIN_FRACTION_DIGITS
        maximumFractionDigits = maxFractionDigits
    }.format(this)
}

fun Number.beautifySATFormat(locale: Locale = Locale.US): String {
    return DecimalFormat.getNumberInstance(locale).format(this)
}

fun Number.numberFormat(locale: Locale = Locale.US): String {
    return NumberFormat.getNumberInstance(locale).format(this)
}