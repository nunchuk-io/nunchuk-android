package com.nunchuk.android.network

import com.nunchuk.android.core.account.AccountManager
import javax.inject.Inject

class HeaderProviderImpl @Inject constructor(
    private val accountManager: AccountManager
) : HeaderProvider {

    override fun getOsVersion() = "Android Q"

    override fun getDeviceId() = "arm64-ss-ahs"

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = accountManager.getAccount().token

}