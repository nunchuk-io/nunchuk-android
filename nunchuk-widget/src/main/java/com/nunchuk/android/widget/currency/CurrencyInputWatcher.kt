/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.widget.currency

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import kotlin.math.min

class CurrencyInputWatcher(
    private val editText: EditText,
    private val currencySymbol: String,
    locale: Locale,
    private val maxNumberOfDecimalPlaces: Int = 8
) : TextWatcher {

    init {
        if (maxNumberOfDecimalPlaces < 1) {
            throw IllegalArgumentException("Maximum number of Decimal Digits must be a positive integer")
        }
    }

    companion object {
        const val FRACTION_FORMAT_PATTERN_PREFIX = "#,##0."
    }

    private var hasDecimalPoint = false
    private val wholeNumberDecimalFormat =
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            applyPattern("#,##0")
        }

    private val fractionDecimalFormat = (NumberFormat.getNumberInstance(locale) as DecimalFormat)

    val decimalFormatSymbols: DecimalFormatSymbols
        get() = wholeNumberDecimalFormat.decimalFormatSymbols

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        fractionDecimalFormat.isDecimalSeparatorAlwaysShown = true
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        hasDecimalPoint = s.toString().contains(decimalFormatSymbols.decimalSeparator.toString())
    }

    @SuppressLint("SetTextI18n")
    override fun afterTextChanged(s: Editable) {
        val newInputString = s.toString()
        val isParsableString = try {
            fractionDecimalFormat.parse(newInputString)!!
            true
        } catch (e: ParseException) {
            false
        }

        if (newInputString.length < currencySymbol.length && !isParsableString) {
            editText.setText(currencySymbol)
            editText.setSelection(currencySymbol.length)
            return
        }

        if (newInputString == currencySymbol) {
            editText.setSelection(currencySymbol.length)
            return
        }

        editText.removeTextChangedListener(this)
        val startLength = editText.text.length
        try {
            var numberWithoutGroupingSeparator =
                parseMoneyValue(
                    newInputString,
                    decimalFormatSymbols.groupingSeparator.toString(),
                    currencySymbol
                )
            if (numberWithoutGroupingSeparator == decimalFormatSymbols.decimalSeparator.toString()) {
                numberWithoutGroupingSeparator = "0$numberWithoutGroupingSeparator"
            }

            numberWithoutGroupingSeparator = truncateNumberToMaxDecimalDigits(
                numberWithoutGroupingSeparator,
                maxNumberOfDecimalPlaces,
                decimalFormatSymbols.decimalSeparator
            )

            val parsedNumber = fractionDecimalFormat.parse(numberWithoutGroupingSeparator)!!
            val selectionStartIndex = editText.selectionStart
            if (hasDecimalPoint) {
                fractionDecimalFormat.applyPattern(
                    FRACTION_FORMAT_PATTERN_PREFIX +
                            getFormatSequenceAfterDecimalSeparator(numberWithoutGroupingSeparator)
                )
                editText.setText("$currencySymbol${fractionDecimalFormat.format(parsedNumber)}")
            } else {
                editText.setText("$currencySymbol${wholeNumberDecimalFormat.format(parsedNumber)}")
            }
            val endLength = editText.text.length
            val selection = selectionStartIndex + (endLength - startLength)
            if (selection > 0 && selection <= editText.text.length) {
                editText.setSelection(selection)
            } else {
                editText.setSelection(editText.text.length - 1)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        editText.addTextChangedListener(this)
    }

    /**
     * @param number the original number to format
     * @return the appropriate zero sequence for the input number. e.g 156.1 returns "0",
     *  14.98 returns "00"
     */
    private fun getFormatSequenceAfterDecimalSeparator(number: String): String {
        val noOfCharactersAfterDecimalPoint = number.length - number.indexOf(decimalFormatSymbols.decimalSeparator) - 1
        return "0".repeat(min(noOfCharactersAfterDecimalPoint, maxNumberOfDecimalPlaces))
    }
}
