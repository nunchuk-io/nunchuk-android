package com.nunchuk.android.core.util

import com.nunchuk.android.core.entities.BTC
import com.nunchuk.android.core.entities.BTC_AND_FIXED_PRECISION
import com.nunchuk.android.core.entities.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.entities.SAT
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import kotlin.math.roundToLong

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getUSDAmount() = balance.getUSDAmount()

fun Amount.getBTCAmount() = when(CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> "${value.beautifySATFormat()} SAT"
    BTC -> "${formattedValue.toDouble().toLong().numberFormat()} BTC"
    else -> "$formattedValue BTC"
}

fun Amount.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

fun Double.fromBTCToUSD() = this * BTC_USD_EXCHANGE_RATE

fun Double.fromUSDToBTC() = this / BTC_USD_EXCHANGE_RATE

fun Amount.pureBTC() = value * SATOSHI_BTC_EXCHANGE_RATE

fun Double.getBTCAmount() = when(CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> "${(this * BTC_SATOSHI_EXCHANGE_RATE).beautifySATFormat()} SAT"
    BTC -> "${toLong().numberFormat()} BTC"
    else -> " ${formatDecimal()} BTC"
}
// = " ${formatDecimal()} BTC"

fun Double.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

private fun Amount.fromBTCToUSD() = value * SATOSHI_BTC_EXCHANGE_RATE * BTC_USD_EXCHANGE_RATE

fun Double.fromBTCToSAT() = (this * SATOSHI_BTC_EXCHANGE_RATE)

fun Double.toAmount() = Amount().copy(value = (this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong())

fun Int.toAmount() = Amount().copy(value = this.toLong())
