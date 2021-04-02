package com.nunchuk.android.network

interface HeaderProvider {

    fun getOsVersion(): String

    fun getDeviceId(): String

    fun getAppVersion(): String

    fun getAccessToken(): String

}