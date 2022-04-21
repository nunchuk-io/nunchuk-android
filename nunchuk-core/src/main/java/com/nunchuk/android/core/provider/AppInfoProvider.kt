package com.nunchuk.android.core.provider

interface AppInfoProvider {

    fun getAppVersion(): String

    fun getAppVersionName(): String

    fun getAppVersionCode(): Int

    fun getAppName(): String

}