package com.nunchuk.android.widget.currency

import java.text.NumberFormat
import java.text.ParseException
import java.util.*

internal fun parseMoneyValue(
    value: String,
    groupingSeparator: String,
    currencySymbol: String
): String =
    value.replace(groupingSeparator, "").replace(currencySymbol, "")

internal fun parseMoneyValueWithLocale(
    locale: Locale,
    value: String,
    groupingSeparator: String,
    currencySymbol: String
): Number {

    val valueWithoutSeparator = parseMoneyValue(value, groupingSeparator, currencySymbol)
    return try {
        NumberFormat.getInstance(locale).parse(valueWithoutSeparator)!!
    } catch (exception: ParseException) {
        0
    }
}

internal fun getLocaleFromTag(localeTag: String): Locale {
    return try {
        Locale.Builder().setLanguageTag(localeTag).build()
    } catch (e: IllformedLocaleException) {
        Locale.getDefault()
    }
}