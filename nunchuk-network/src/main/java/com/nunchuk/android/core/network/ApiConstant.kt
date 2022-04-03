package com.nunchuk.android.core.network

internal object ApiConstant {
    private const val API_VERSION = "v1.1"
    internal const val BASE_URL = "https://api.nunchuk.io/$API_VERSION/"
    internal const val BASE_URL_MATRIX = "https://matrix.nunchuk.io/"

    internal const val HTTP_CONNECT_TIMEOUT = 5L
    internal const val HTTP_READ_TIMEOUT = 5L

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