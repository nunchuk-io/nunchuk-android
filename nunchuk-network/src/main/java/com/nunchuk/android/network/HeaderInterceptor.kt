package com.nunchuk.android.network

import com.nunchuk.android.network.ApiConstant.HEADER_APP_VERSION
import com.nunchuk.android.network.ApiConstant.HEADER_DEVICE_ID
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(
    private val headerProvider: HeaderProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_DEVICE_ID, headerProvider.getDeviceId())
            .header(HEADER_APP_VERSION, headerProvider.getAppVersion())
            //.header(HEADER_CONTENT_TYPE, "application/json;charset=utf8")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("accept", "application/json;charset=UTF-8")
            //.header(HEADER_TOKEN_TYPE, "Bearer " + headerProvider.getAccessToken())
            .build()
        return chain.proceed(request)
    }

}