package com.nunchuk.android.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings.Secure
import com.nunchuk.android.core.account.AccountManager
import javax.inject.Inject

class HeaderProviderImpl @Inject constructor(
    context: Context,
    private val accountManager: AccountManager
) : HeaderProvider {

    @SuppressLint("HardwareIds")
    private val deviceId: String = Secure.getString(context.applicationContext.contentResolver, Secure.ANDROID_ID)

    override fun getOsVersion(): String = if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        VERSION.BASE_OS
    } else {
        VERSION.RELEASE
    }

    override fun getDeviceId() = deviceId

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = accountManager.getAccount().token

}