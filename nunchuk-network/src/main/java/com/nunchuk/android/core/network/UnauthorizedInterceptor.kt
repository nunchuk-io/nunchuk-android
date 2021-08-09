package com.nunchuk.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class UnauthorizedInterceptor @Inject constructor() : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isUnauthorized()) {
            UnauthorizedEventBus.instance().publish()
        }
        return response
    }

}

internal fun Response.isUnauthorized(): Boolean {
    return code() == ApiErrorCode.UNAUTHORIZED
}
