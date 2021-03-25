package com.nunchuk.android.network

interface HeaderProvider {

    fun getDeviceId(): String

    fun getAppVersion(): String

    fun getAccessToken(): String

}