package com.nunchuk.android.core.util

import com.nunchuk.android.core.network.UNKNOWN_ERROR
import com.nunchuk.android.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

fun Exception.messageOrUnknownError() = message.orUnknownError()

fun String?.orUnknownError() = this ?: UNKNOWN_ERROR

var BTC_USD_EXCHANGE_RATE = 45000.0

const val SATOSHI_BTC_EXCHANGE_RATE = 0.00000001

fun Long.formatDate(): String = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm aaa", Locale.US).format(Date(this * 1000))

fun Transaction.getFormatDate(): String = if (blockTime <= 0) "--/--/--" else (blockTime).formatDate()

fun String.fromMxcUriToMatrixDownloadUrl(): String {
    if (this.isEmpty()) {
        return ""
    }

    // Sample: https://matrix.nunchuk.io/_matrix/media/r0/download/nunchuk.io/occyhYuhbbpkHNbJLZwwdtuf
    val contentUriInfo = this.removePrefix("mxc://").split("/")

    val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
    val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
    return BASE_DOWNLOAD_URL_MATRIX.plus(serverName).plus("/").plus(mediaId)
}
internal const val BASE_DOWNLOAD_URL_MATRIX = "https://matrix.nunchuk.io/_matrix/media/r0/download/"
