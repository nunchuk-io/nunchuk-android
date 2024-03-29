/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

const val USD_FRACTION_DIGITS = 2

const val MIN_FRACTION_DIGITS = 2
const val MAX_FRACTION_DIGITS = 8

fun Number.formatDecimal(minFractionDigits: Int = MIN_FRACTION_DIGITS, maxFractionDigits: Int = MAX_FRACTION_DIGITS): String {
    return DecimalFormat("#,##0.00").apply {
        minimumFractionDigits = minFractionDigits
        maximumFractionDigits = maxFractionDigits
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
    }.format(this)
}

fun Number.formatDecimalWithoutZero(maxFractionDigits: Int = MAX_FRACTION_DIGITS): String {
    return DecimalFormat("#,###.##").apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxFractionDigits
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
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