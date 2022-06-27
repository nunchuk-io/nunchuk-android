package com.nunchuk.android.app.provider

import android.content.Context
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.utils.CrashlyticsReporter
import javax.inject.Inject

class AppInfoProviderImpl @Inject constructor(
    private val context: Context
) : AppInfoProvider {

    override fun getAppVersion() = "${getAppVersionName()} (${getAppVersionCode()})"

    override fun getAppVersionName() = BuildConfig.VERSION_NAME

    override fun getAppVersionCode() = BuildConfig.VERSION_CODE

    override fun getAppName() = try {
        val appPackageName = context.applicationContext.packageName
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(appPackageName, 0)
        var appName = pm.getApplicationLabel(appInfo).toString()
        if (!appName.matches("\\A\\p{ASCII}*\\z".toRegex())) {
            appName = appPackageName
        }
        appName
    } catch (e: Throwable) {
        CrashlyticsReporter.recordException(e)
        "Nunchuk"
    }

}