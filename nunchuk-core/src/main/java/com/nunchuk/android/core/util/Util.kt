package com.nunchuk.android.core.util

import com.nunchuk.android.core.network.UNKNOWN_ERROR
import com.nunchuk.android.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

fun Exception.messageOrUnknownError() = message.orUnknownError()

fun String?.orUnknownError() = this ?: UNKNOWN_ERROR

const val BTC_USD_EXCHANGE_RATE = 45000

const val SATOSHI_BTC_EXCHANGE_RATE = 0.00000001

fun Long.formatDate(): String = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm aaa", Locale.US).format(Date(this * 1000))

fun Transaction.getFormatDate(): String = if (blockTime <= 0) "--/--/--" else (blockTime).formatDate()
