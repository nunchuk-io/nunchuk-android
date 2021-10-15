package com.nunchuk.android.app.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.Secure.getString
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.network.HeaderProvider
import javax.inject.Inject

class HeaderProviderImpl @Inject constructor(
    context: Context,
    private val accountManager: AccountManager
) : HeaderProvider {

    @SuppressLint("HardwareIds")
    private val deviceId: String = getString(context.applicationContext.contentResolver, ANDROID_ID)

    override fun getOsVersion(): String = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        VERSION.BASE_OS
    } else {
        VERSION.RELEASE
    }

    override fun getDeviceId() = deviceId

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = accountManager.getAccount().token

}