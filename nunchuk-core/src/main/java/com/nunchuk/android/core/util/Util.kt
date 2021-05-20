package com.nunchuk.android.core.util

fun String?.orUnknownError() = this ?: "Unknown Error"

const val BTC_USD_EXCHANGE_RATE = 45000

const val SATOSHI_BTC_EXCHANGE_RATE = 0.00000001