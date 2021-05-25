package com.nunchuk.android.core.util

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getUSDAmount() = balance.getUSDAmount()

fun Wallet.getConfiguration() = "${totalRequireSigns}/${signers.size}"

fun Amount.getBTCAmount() = "$formattedValue BTC"

fun Amount.getUSDAmount() = "$${value * SATOSHI_BTC_EXCHANGE_RATE * BTC_USD_EXCHANGE_RATE} USD"

fun Double.fromBTCToUSD() = this * BTC_USD_EXCHANGE_RATE

fun Double.fromUSDToBTC() = this / BTC_USD_EXCHANGE_RATE

fun Amount.pureBTC() = value * SATOSHI_BTC_EXCHANGE_RATE