/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

internal object ApiConstant {
    private const val API_VERSION = "v1.1"
    internal const val BASE_URL = "https://api.nunchuk.io/$API_VERSION/"
    internal const val BASE_TEST_NET_URL = "https://api-testnet.nunchuk.io/$API_VERSION/"
    internal const val BASE_URL_MATRIX = "https://matrix.nunchuk.io/"

    internal const val HTTP_CONNECT_TIMEOUT = 30L
    internal const val HTTP_READ_TIMEOUT = 30L
    internal const val HTTP_WRITE_TIMEOUT = 30L

    internal const val HEADER_TOKEN_TYPE = "Authorization"
    internal const val HEADER_DEVICE_ID = "x-nc-device-id"
    internal const val HEADER_APP_VERSION = "x-nc-app-version"
    internal const val HEADER_OS_VERSION = "x-nc-os-version"
    internal const val HEADER_OS_NAME = "x-nc-os-name"
    internal const val HEADER_DEVICE_CLASS = "x-nc-device-class"
    internal const val HEADER_CONTENT_TYPE = "Content-Type"
    internal const val HEADER_ACCEPT = "accept"

    internal const val HEADER_CONTENT_TYPE_VALUE = "application/json"
    internal const val HEADER_DEVICE_CLASS_VALUE = "Mobile"
    internal const val HEADER_OS_NAME_VALUE = "Android"
}