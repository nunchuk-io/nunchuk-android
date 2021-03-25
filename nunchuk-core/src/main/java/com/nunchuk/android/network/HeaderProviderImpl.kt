package com.nunchuk.android.network

import javax.inject.Inject

class HeaderProviderImpl @Inject constructor() : HeaderProvider {

    override fun getDeviceId() = "arm64-ss-ahs"

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = "abxcdefgh"

}