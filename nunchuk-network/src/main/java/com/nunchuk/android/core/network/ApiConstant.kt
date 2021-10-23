package com.nunchuk.android.core.network

internal object ApiConstant {
    // FIXME move api version to endpoint url instead
    internal const val BASE_URL = "https://api.nunchuk.io/v1/"
    internal const val BASE_URL_V1_1 = "https://api.nunchuk.io/v1.1/"

    internal const val HTTP_CONNECT_TIMEOUT = 60L
    internal const val HTTP_READ_TIMEOUT = 60L

    internal const val HEADER_TOKEN_TYPE = "Authorization"
    internal const val HEADER_DEVICE_ID = "x-nc-device-id"
    internal const val HEADER_APP_VERSION = "x-nc-app-version"
    internal const val HEADER_OS_VERSION = "x-nc-os-version"
    internal const val HEADER_CONTENT_TYPE = "Content-Type"
}