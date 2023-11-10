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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.util.Linkify
import android.widget.TextView
import com.nunchuk.android.core.R
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.network.UNKNOWN_ERROR
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.utils.CrashlyticsReporter
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

fun Throwable.readableMessage() = message ?: UNKNOWN_ERROR

fun Exception.messageOrUnknownError() = message.orUnknownError()

fun String?.orUnknownError() = this ?: UNKNOWN_ERROR

fun String.isValidCvc() = length in MIN_CVC_LENGTH..MAX_CVC_LENGTH

const val MAX_NOTE_LENGTH = 280

const val MAX_CVC_LENGTH = 32
const val MIN_CVC_LENGTH = 6

const val CHAIN_CODE_LENGTH = 64

var BTC_CURRENCY_EXCHANGE_RATE = 45000.0

var BLOCKCHAIN_STATUS: ConnectionStatus? = null

const val TWITTER_LINK = "https://twitter.com/nunchuk_io"
const val CONTACT_EMAIL = "contact@nunchuk.io"

const val SATOSHI_BTC_EXCHANGE_RATE = 0.00000001
const val BTC_SATOSHI_EXCHANGE_RATE = 100000000

const val NFC_CARD_TIMEOUT = 5000
const val NFC_DEFAULT_NAME = "NFC Key"
const val TAPSIGNER_INHERITANCE_NAME = "TAPSIGNER (inh.)"

const val ONE_HOUR_TO_SECONDS = 60 * 60

const val SIGNER_PATH_PREFIX = "m/48h"
const val COLDCARD_GUIDE_URL = "https://coldcard.com/docs/quick"
const val COLDCARD_DEFAULT_KEY_NAME = "COLDCARD"

const val SUPPORT_ROOM_USER_ID = "@support:nunchuk.io"
const val SUPPORT_ROOM_TYPE = "io.nunchuk.support"
const val GROUP_CHAT_ROOM_TYPE = "io.nunchuk.premium"
const val SUPPORT_TEST_NET_ROOM_TYPE = "io.nunchuk.support.testnet"
const val RENEW_ACCOUNT_LINK = "https://nunchuk.io/my-account"

const val DEFAULT_COLDCARD_WALLET_NAME = "My COLDCARD wallet"
const val DELAY_DYNAMIC_QR = 1000L

const val LOW_DENSITY = 50
const val MEDIUM_DENSITY = 100
const val HIGH_DENSITY = 200

const val USD_CURRENCY = "USD"

var LOCAL_CURRENCY = USD_CURRENCY

fun Int.densityToLevel() : Float = when(this) {
    LOW_DENSITY -> 0f
    MEDIUM_DENSITY -> 1f
    else -> 2f
}

fun Double.formatRoundDecimal(): String {
    val df = DecimalFormat("#.##")
    return df.format(this)
}

fun String.fixAfterDecimal(count: Int = 2) : String{
    val decimalIndex = indexOf(".")
    return if (decimalIndex > 0) {
        substring(0, minOf(length, decimalIndex + count + 1))
    } else {
        this
    }
}

fun Double.roundDecimal(): Double = formatRoundDecimal().toDoubleOrNull() ?: 0.0

fun Long.formatDate(): String = SimpleDateFormat("MM/dd/yyyy 'at' HH:mm aaa", Locale.US).format(Date(this * 1000))

fun Transaction.getFormatDate(): String = if (blockTime <= 0) "--/--/--" else (blockTime).formatDate()
fun Long.getBtcFormatDate(): String = if (this <= 0) "--/--/--" else (this).formatDate()

fun String.fromMxcUriToMatrixDownloadUrl(): String {
    if (this.isEmpty()) return ""

    // Sample: https://matrix.nunchuk.io/_matrix/media/r0/download/nunchuk.io/occyhYuhbbpkHNbJLZwwdtuf
    val contentUriInfo = this.removePrefix("mxc://").split("/")

    val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
    val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
    return BASE_DOWNLOAD_URL_MATRIX.plus(serverName).plus("/").plus(mediaId)
}

internal const val BASE_DOWNLOAD_URL_MATRIX = "https://matrix.nunchuk.io/_matrix/media/r0/download/"

fun InputStream.saveToFile(file: String) = try {
    use { input ->
        File(file).outputStream().use { output ->
            input.copyTo(output)
        }
    }
} catch (t: Throwable) {
    CrashlyticsReporter.recordException(t)
    0
}

fun TextView.linkify(textToLink: String, url: String) {
    val pattern = Pattern.compile(textToLink)
    Linkify.addLinks(this, pattern, url, { _, _, _ -> true }, { _, _ -> "" })
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip =
        ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

fun Context.getTextBtcUnit() = when (CURRENT_DISPLAY_UNIT_TYPE) {
    SAT -> getString(R.string.nc_currency_sat)
    else ->getString(R.string.nc_currency_btc)
}

