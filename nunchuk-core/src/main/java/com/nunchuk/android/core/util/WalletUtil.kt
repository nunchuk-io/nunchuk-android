package com.nunchuk.android.core.util

import com.nunchuk.android.core.domain.data.BTC
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.domain.di.NativeSdkProvider
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToLong

fun Wallet.getBTCAmount() = balance.getBTCAmount()

fun Wallet.getUSDAmount() = balance.getUSDAmount()

fun Amount.getBTCAmount() = when (CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> "${value.beautifySATFormat()} sat"
    BTC -> "${NativeSdkProvider.instance.nativeSdk.valueFromAmount(this)} BTC"
    else -> "$formattedValue BTC"
}

fun Amount.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

fun Double.fromBTCToUSD() = this * BTC_USD_EXCHANGE_RATE

fun Double.fromUSDToBTC() = this / BTC_USD_EXCHANGE_RATE

fun Amount.pureBTC() = value.toDouble().fromSATtoBTC()

fun Double.getBTCAmount() = when (CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> "${((this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong()).beautifySATFormat()} sat"
    BTC -> "${toLong().numberFormat()} BTC"
    else -> " ${formatDecimal()} BTC"
}

fun Double.getUSDAmount() = "$${fromBTCToUSD().formatDecimal(USD_FRACTION_DIGITS)}"

private fun Amount.fromBTCToUSD() = value * SATOSHI_BTC_EXCHANGE_RATE * BTC_USD_EXCHANGE_RATE

fun Double.fromSATtoBTC() = (this * SATOSHI_BTC_EXCHANGE_RATE)

fun Double.toAmount() = Amount().copy(value = (this * BTC_SATOSHI_EXCHANGE_RATE).roundToLong())

fun Int.toAmount() = Amount().copy(value = this.toLong())

fun String.toNumericValue(locale: Locale = Locale.US): Number = try {
    NumberFormat.getInstance(locale).parse(this) ?: 0.0
} catch (t: Exception) {
    0.0
}

fun AddressType.isTaproot() = this == AddressType.TAPROOT
