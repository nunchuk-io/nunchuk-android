package com.nunchuk.android.core.util

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import kotlin.math.roundToLong

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getUSDAmount() = balance.getUSDAmount()

fun Amount.getBTCAmount() = "$formattedValue BTC"

fun Amount.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

fun Double.fromBTCToUSD() = this * BTC_USD_EXCHANGE_RATE

fun Double.fromUSDToBTC() = this / BTC_USD_EXCHANGE_RATE

fun Amount.pureBTC() = value * SATOSHI_BTC_EXCHANGE_RATE

fun Double.getBTCAmount() = " ${formatDecimal()} BTC"

fun Double.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

private fun Amount.fromBTCToUSD() = value * SATOSHI_BTC_EXCHANGE_RATE * BTC_USD_EXCHANGE_RATE

fun Double.toAmount() = Amount().copy(value = (this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong())

fun Int.toAmount() = Amount().copy(value = this.toLong())
