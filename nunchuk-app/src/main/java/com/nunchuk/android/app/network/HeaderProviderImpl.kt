package com.nunchuk.android.app.network

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.utils.DeviceManager
import javax.inject.Inject

class HeaderProviderImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val deviceManager: DeviceManager
) : HeaderProvider {

    override fun getOsVersion(): String = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        VERSION.BASE_OS
    } else {
        VERSION.RELEASE
    }

    override fun getDeviceId() = deviceManager.getDeviceId()

    override fun getDeviceName(): String = android.os.Build.MODEL

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = accountManager.getAccount().token

}