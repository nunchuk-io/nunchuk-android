package com.nunchuk.android.core.util

object CurrencyFormatter {
    fun format(value: String, maxAfterDigit: Int = 10): String {
        if (value.isNotEmpty() && value.last() == ',') return value.dropLast(1)
        if (value.count { c -> c == '.' } > 1) return value.dropLast(1)
        val dotIndex = value.indexOf('.')
        if (dotIndex in value.indices) {
            if (value.lastIndex - dotIndex > maxAfterDigit) return value.dropLast(1)
        }
        return value
    }
}