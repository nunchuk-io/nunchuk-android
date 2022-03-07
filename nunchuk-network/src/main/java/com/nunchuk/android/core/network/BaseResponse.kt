package com.nunchuk.android.core.network

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Data<out T>(
    @SerializedName("data")
    private val _data: T?,
    @SerializedName("error")
    private val _error: ErrorResponse?
) : Serializable {

    val data: T
        get() {
            if (_data != null) return _data
            _error?.apply {
                if (code == ApiErrorCode.UNAUTHORIZED) {
                    UnauthorizedEventBus.instance().publish()
                }
                if (code != 0) {
                    throw NunchukApiException(
                        code = code,
                        message = message ?: UNKNOWN_ERROR,
                        errorDetail = _error.details
                    )
                }
                throw ApiInterceptedException()
            }
            throw NunchukApiException()
        }

    fun getError(): NunchukApiException? {
        if (_error != null && _error.code != 0) {
            return NunchukApiException(
                code = _error.code,
                message = _error.message ?: UNKNOWN_ERROR,
                errorDetail = _error.details
            )
        }
        return null
    }
}

data class ErrorResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String?,
    @SerializedName("details")
    val details: ErrorDetail? = null
) : Serializable

class UnauthorizedException : Exception()

class ApiInterceptedException : Exception()
data class ErrorDetail(
    @SerializedName("halfToken")
    val halfToken: String? = null,
    @SerializedName("deviceID")
    val deviceID: String? = null
)

class NunchukApiException(val code: Int = 0, override val message: String = UNKNOWN_ERROR, val errorDetail: ErrorDetail? = null) : Exception(message)

fun NunchukApiException.accountExisted() = code == ApiErrorCode.ACCOUNT_EXISTED

object ApiErrorCode {
    const val ACCOUNT_EXISTED = -100
    const val NEW_DEVICE = 841
    const val UNAUTHORIZED = 401
}

const val UNKNOWN_ERROR = "Unknown error"


