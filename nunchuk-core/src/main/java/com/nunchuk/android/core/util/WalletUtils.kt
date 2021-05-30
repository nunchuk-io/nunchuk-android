package com.nunchuk.android.core.util

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.utils.formatDecimal

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getUSDAmount() = balance.getUSDAmount()

fun Wallet.getConfiguration() = "${totalRequireSigns}/${signers.size}"

fun Amount.getBTCAmount() = "$formattedValue BTC"

fun Amount.getUSDAmount() = "$${fromBTCToUSD().formatDecimal()} USD"

fun Double.fromBTCToUSD() = this * BTC_USD_EXCHANGE_RATE

fun Double.fromUSDToBTC() = this / BTC_USD_EXCHANGE_RATE

fun Amount.pureBTC() = value * SATOSHI_BTC_EXCHANGE_RATE

fun Double.getBTCAmount() = " ${formatDecimal()} BTC"

fun Double.getUSDAmount() = "$${fromBTCToUSD().formatDecimal()} USD"

private fun Amount.fromBTCToUSD() = value * SATOSHI_BTC_EXCHANGE_RATE * BTC_USD_EXCHANGE_RATE

fun Double.toAmount() = Amount().copy(value = (this / SATOSHI_BTC_EXCHANGE_RATE).toLong())

fun Int.toAmount() = Amount().copy(value = this.toLong())
