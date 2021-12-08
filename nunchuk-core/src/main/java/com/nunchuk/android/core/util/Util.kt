package com.nunchuk.android.core.util

import android.text.util.Linkify
import android.widget.TextView
import com.nunchuk.android.core.network.UNKNOWN_ERROR
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

fun Throwable.readableMessage() = message ?: UNKNOWN_ERROR

fun Exception.messageOrUnknownError() = message.orUnknownError()

fun String?.orUnknownError() = this ?: UNKNOWN_ERROR

var BTC_USD_EXCHANGE_RATE = 45000.0

var BLOCKCHAIN_STATUS: ConnectionStatus? = null

const val SATOSHI_BTC_EXCHANGE_RATE = 0.00000001
const val BTC_SATOSHI_EXCHANGE_RATE = 100000000

fun Long.formatDate(): String = SimpleDateFormat("MM/dd/yyyy 'at' HH:mm aaa", Locale.US).format(Date(this * 1000))

fun Transaction.getFormatDate(): String = if (blockTime <= 0) "--/--/--" else (blockTime).formatDate()

fun String.fromMxcUriToMatrixDownloadUrl(): String {
    if (this.isEmpty()) return ""

    // Sample: https://matrix.nunchuk.io/_matrix/media/r0/download/nunchuk.io/occyhYuhbbpkHNbJLZwwdtuf
    val contentUriInfo = this.removePrefix("mxc://").split("/")

    val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
    val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
    return BASE_DOWNLOAD_URL_MATRIX.plus(serverName).plus("/").plus(mediaId)
}

internal const val BASE_DOWNLOAD_URL_MATRIX = "https://matrix.nunchuk.io/_matrix/media/r0/download/"

fun InputStream.saveToFile(file: String) = use { input ->
    File(file).outputStream().use { output ->
        input.copyTo(output)
    }
}

internal fun Chain.isMainNet() = this == Chain.MAIN

fun TextView.linkify(textToLink: String, url: String) {
    val pattern = Pattern.compile(textToLink)
    Linkify.addLinks(this, pattern, url, { _, _, _ -> true }, { _, _ -> "" })
}