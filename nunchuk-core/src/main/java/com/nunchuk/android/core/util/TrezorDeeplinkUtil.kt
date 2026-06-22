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

import android.net.Uri

const val TREZOR_CALLBACK_SCHEME = "nunchuk"
const val TREZOR_CALLBACK_HOST = "trezor"

private const val QUERY_METHOD = "method"
private const val QUERY_RESPONSE = "response"

object TrezorCallbackMethod {
    const val GET_PUBLIC_KEY = "getPublicKey"
    const val SIGN_TRANSACTION = "signTransaction"
    const val SIGN_MESSAGE = "signMessage"
    const val GET_ADDRESS = "getAddress"
}

data class TrezorCallbackPayload(
    val rawUri: String,
    val method: String,
    val response: String,
    val xfp: String,
    val walletId: String,
    val txId: String,
    val path: String,
    val message: String,
    val address: String,
)

fun isTrezorCallbackUri(uri: Uri?): Boolean {
    return uri?.scheme == TREZOR_CALLBACK_SCHEME && uri.host == TREZOR_CALLBACK_HOST
}

fun parseTrezorCallback(uriString: String?): TrezorCallbackPayload? {
    if (uriString.isNullOrBlank()) return null
    return parseTrezorCallback(Uri.parse(uriString))
}

fun parseTrezorCallback(uri: Uri?): TrezorCallbackPayload? {
    if (!isTrezorCallbackUri(uri)) return null
    val safeUri = uri ?: return null
    val method = safeUri.getQueryParameter(QUERY_METHOD).orEmpty()
    if (method.isBlank()) return null

    return TrezorCallbackPayload(
        rawUri = safeUri.toString(),
        method = method,
        response = safeUri.getQueryParameter(QUERY_RESPONSE).orEmpty(),
        xfp = safeUri.getQueryParameter("xfp").orEmpty(),
        walletId = safeUri.getQueryParameter("wallet_id").orEmpty(),
        txId = safeUri.getQueryParameter("txid").orEmpty(),
        path = safeUri.getQueryParameter("path").orEmpty(),
        message = safeUri.getQueryParameter("message").orEmpty(),
        address = safeUri.getQueryParameter("address").orEmpty(),
    )
}
