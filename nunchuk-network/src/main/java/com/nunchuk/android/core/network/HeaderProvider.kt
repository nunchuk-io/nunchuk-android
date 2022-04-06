package com.nunchuk.android.core.network

import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_CLASS_VALUE
import com.nunchuk.android.core.network.ApiConstant.HEADER_OS_NAME_VALUE

interface HeaderProvider {

    fun getOsVersion(): String

    fun getDeviceId(): String

    fun getDeviceName(): String

    fun getAppVersion(): String

    fun getAccessToken(): String

    fun getOSName() = HEADER_OS_NAME_VALUE

    fun getDeviceClass() = HEADER_DEVICE_CLASS_VALUE

}