package com.nunchuk.android.network

internal object ApiConstant {
    internal const val BASE_URL = "https://api.nunchuk.io/v1/"

    internal const val HTTP_CONNECT_TIMEOUT = 60L
    internal const val HTTP_READ_TIMEOUT = 60L

    internal const val HEADER_TOKEN_TYPE = "Authorization"
    internal const val HEADER_DEVICE_ID = "x-device-id"
    internal const val HEADER_APP_VERSION = "x-app-version"
    internal const val HEADER_OS_VERSION = "x-os-version"
    internal const val HEADER_CONTENT_TYPE = "Content-Type"
}