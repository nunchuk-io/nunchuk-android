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

package com.nunchuk.android.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import java.text.DecimalFormatSymbols

class NumberCommaTransformation(
    suffix: String = "",
) : VisualTransformation {
    private val formatSuffix = if (suffix.isEmpty()) "" else " $suffix"
    override fun filter(text: AnnotatedString): TransformedText {
        val splits = text.split("${DecimalFormatSymbols.getInstance().decimalSeparator}")
        val formatValue = splits[0].toLongOrNull()
        val value = when {
            formatValue == null -> ""
            splits.size > 1 -> "${formatValue.formatDecimalWithoutZero()}${DecimalFormatSymbols.getInstance().decimalSeparator}${
                splits[1]
            }$formatSuffix"

            else -> "${formatValue.formatDecimalWithoutZero()}$formatSuffix"
        }
        return TransformedText(
            text = AnnotatedString(value),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return value.length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return text.length
                }
            }
        )
    }
}