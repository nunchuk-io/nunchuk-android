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

import java.text.DecimalFormatSymbols

object CurrencyFormatter {
    private val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
    fun format(value: String, maxAfterDigit: Int = 10): String {
        if (value.isNotEmpty() && !(value.last() == decimalSeparator || value.last().isDigit())) return value.dropLast(1)
        if (value.count { c -> c == decimalSeparator } > 1) return value.dropLast(1)
        val dotIndex = value.indexOf(decimalSeparator)
        if (dotIndex in value.indices) {
            if (value.lastIndex - dotIndex > maxAfterDigit) return value.dropLast(1)
        }
        return value
    }
}