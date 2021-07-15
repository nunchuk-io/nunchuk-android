package com.nunchuk.android.app.provider

import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.provider.AppInfoProvider
import javax.inject.Inject

internal class AppInfoProviderImpl @Inject constructor() : AppInfoProvider {

    override fun getAppVersion(): String {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        return "$versionName ($versionCode)"
    }

}