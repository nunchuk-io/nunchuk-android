package com.nunchuk.android.core.network

import com.nunchuk.android.core.network.ApiConstant.HEADER_APP_VERSION
import com.nunchuk.android.core.network.ApiConstant.HEADER_CONTENT_TYPE
import com.nunchuk.android.core.network.ApiConstant.HEADER_DEVICE_ID
import com.nunchuk.android.core.network.ApiConstant.HEADER_OS_VERSION
import com.nunchuk.android.core.network.ApiConstant.HEADER_TOKEN_TYPE
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(
    private val headerProvider: HeaderProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .validHeader(HEADER_DEVICE_ID, headerProvider.getDeviceId())
            .validHeader(HEADER_APP_VERSION, headerProvider.getAppVersion())
            .validHeader(HEADER_OS_VERSION, headerProvider.getOsVersion())
            .validHeader(HEADER_CONTENT_TYPE, "application/json")
            .validHeader("accept", "application/json;charset=UTF-8")
            .validHeader(HEADER_TOKEN_TYPE, "Bearer " + headerProvider.getAccessToken())
            .build()
        return chain.proceed(request)
    }

}

internal fun Request.Builder.validHeader(name: String, value: String?): Request.Builder {
    if (!value.isNullOrEmpty()) {
        header(name, value)
    }
    return this
}
