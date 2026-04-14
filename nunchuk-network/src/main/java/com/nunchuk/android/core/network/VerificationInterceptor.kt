package com.nunchuk.android.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject

class VerificationInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!isVerifiableEndpoint(request.url.encodedPath)) return response

        val errorCode = parseErrorCode(response)
        if (errorCode != ApiErrorCode.VERIFICATION_REQUIRED) return response

        val token = runBlocking {
            try {
                withTimeout(VERIFICATION_TIMEOUT_MS) {
                    VerificationEventBus.instance().requestVerificationToken()
                }
            } catch (_: Exception) {
                null
            }
        } ?: return response

        response.close()

        val retryRequest = request.newBuilder()
            .header(HEADER_VERIFICATION_TOKEN, token)
            .build()
        return chain.proceed(retryRequest)
    }

    private fun isVerifiableEndpoint(path: String): Boolean {
        return VERIFIABLE_PATHS.any { path.endsWith(it) }
    }

    private fun parseErrorCode(response: Response): Int {
        return try {
            val body = response.peekBody(PEEK_BODY_LIMIT)
            val json = JSONObject(body.string())
            json.optJSONObject("error")?.optInt("code", 0) ?: 0
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        private const val HEADER_VERIFICATION_TOKEN = "X-Verification-Token"
        private const val PEEK_BODY_LIMIT = 1024L * 1024L
        private const val VERIFICATION_TIMEOUT_MS = 120_000L
        private val VERIFIABLE_PATHS = listOf(
            "/passport/username-availability",
            "/passport/register",
            "/passport/sign-in",
            "/passport/web/sign-in",
        )
    }
}
