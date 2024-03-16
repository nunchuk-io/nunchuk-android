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

import com.nunchuk.android.core.domain.data.BTC
import com.nunchuk.android.core.domain.data.BTC_AND_FIXED_PRECISION
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getCurrencyAmount() = balance.getCurrencyAmount()

fun Amount.getBTCAmount() = pureBTC().getBTCAmount()

fun Double.getBtcSat() =  when (CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> this.roundToLong()
    else -> (this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong()
}

fun Amount.getCurrencyAmount(): String {
    return "${getDisplayCurrency()}${fromBTCToCurrency().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"
}

fun Double.fromBTCToCurrency() = this * BTC_CURRENCY_EXCHANGE_RATE

fun Double.fromCurrencyToBTC() = this / BTC_CURRENCY_EXCHANGE_RATE

fun Amount.pureBTC() = value.toDouble().fromSATtoBTC()

fun Double.getBTCAmount() = when (CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> "${((this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong()).beautifySATFormat()} sat"
    BTC -> "${formatDecimal()} BTC"
    else -> "${formatDecimal(minFractionDigits = MAX_FRACTION_DIGITS)} BTC"
}

fun Double.getBTCAmountWithoutSat() = when (CURRENT_DISPLAY_UNIT_TYPE) {
    BTC_AND_FIXED_PRECISION -> formatDecimal(minFractionDigits = MAX_FRACTION_DIGITS)
    else -> formatDecimal()
}


fun Double.getCurrencyAmount() = "${getDisplayCurrency()}${fromBTCToCurrency().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"

fun getDisplayCurrency() = if (LOCAL_CURRENCY == USD_CURRENCY) "$" else "$LOCAL_CURRENCY "

private fun Amount.fromBTCToCurrency() = value * SATOSHI_BTC_EXCHANGE_RATE * BTC_CURRENCY_EXCHANGE_RATE

fun Double.fromSATtoBTC() = (this * SATOSHI_BTC_EXCHANGE_RATE)

fun Double.toAmount() = Amount().copy(value = (this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong())

fun Int.toAmount() = Amount().copy(value = this.toLong())

fun Long.toAmount() = Amount(value = this)

fun String.toNumericValue(locale: Locale = Locale.US): Number = try {
    NumberFormat.getInstance(locale).parse(this) ?: 0.0
} catch (t: Exception) {
    0.0
}

fun AddressType.isTaproot() = this == AddressType.TAPROOT

fun Iterable<Amount>.sum(): Amount {
    val sum = Amount(0)
    for (element in this) {
        sum.plusAssign(element)
    }
    return sum
}
