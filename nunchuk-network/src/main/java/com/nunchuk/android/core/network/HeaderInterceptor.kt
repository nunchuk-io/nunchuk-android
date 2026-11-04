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

package com.nunchuk.android.core.network

import com.nunchuk.android.core.network.ApiConstant.HEADER_ACCEPT
import com.nunchuk.android.core.network.ApiConstant.HEADER_APP_ID
import com.nunchuk.android.core.network.ApiConstant.HEADER_APP_VERSION
import com.nunchuk.android.core.network.ApiConstant.HEADER_CONTENT_TYPE
import com.nunchuk.android.core.network.ApiConstant.HEADER_CONTENT_TYPE_VALUE
import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_CLASS
import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_ID
import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_NAME
import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_NAME_MAX_LENGTH
import com.nunchuk.android.core.network.ApiConstant.HEADER_OS_NAME
import com.nunchuk.android.core.network.ApiConstant.HEADER_OS_VERSION
import com.nunchuk.android.core.network.ApiConstant.HEADER_TOKEN_TYPE
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(
    private val headerProvider: HeaderProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .validHeader(HEADER_DEVICE_ID, headerProvider.getDeviceId())
            .validHeader(HEADER_DEVICE_NAME, sanitizeDeviceName(headerProvider.getDeviceName()))
            .validHeader(HEADER_APP_VERSION, headerProvider.getAppVersion())
            .validHeader(HEADER_OS_VERSION, headerProvider.getOsVersion())
            .validHeader(HEADER_OS_NAME, headerProvider.getOSName())
            .validHeader(HEADER_DEVICE_CLASS, headerProvider.getDeviceClass())
            .validHeader(HEADER_APP_ID, headerProvider.getApplicationId())
            .validHeader(HEADER_CONTENT_TYPE, HEADER_CONTENT_TYPE_VALUE)
            .validHeader(HEADER_ACCEPT, "application/json;charset=UTF-8")
            .validHeader(HEADER_TOKEN_TYPE, "Bearer " + headerProvider.getAccessToken())
            .build()
        return chain.proceed(request)
    }

}

internal fun Request.Builder.validHeader(name: String, value: String?): Request.Builder {
    if (!value.isNullOrEmpty()) {
        header(name, value)
    }
    return this
}

/**
 * The phone model as a header value, or null to omit the header. OkHttp throws on characters
 * outside printable ASCII, so anything it would reject (control characters, non-ASCII, over-long)
 * drops the header rather than failing the request.
 */
internal fun sanitizeDeviceName(name: String?): String? {
    val value = name?.trim().orEmpty()
    if (value.isEmpty() || value.length > HEADER_DEVICE_NAME_MAX_LENGTH) return null
    if (value.any { it !in '\u0020'..'\u007e' }) return null
    return value
}
