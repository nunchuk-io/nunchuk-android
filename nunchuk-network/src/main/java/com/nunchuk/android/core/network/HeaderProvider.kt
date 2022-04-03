package com.nunchuk.android.core.network

interface HeaderProvider {

    fun getOsVersion(): String

    fun getDeviceId(): String

    fun getDeviceName(): String

    fun getAppVersion(): String

    fun getAccessToken(): String

    fun getOSName(): String

    fun getDeviceClass(): String

}