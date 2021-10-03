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
                    throw NunchukApiException(code, message ?: UNKNOWN_ERROR)
                }
                throw ApiSuccessException()
            }
            throw NunchukApiException()
        }
}

data class ErrorResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String?
) : Serializable

class ApiSuccessException : Exception()

class NunchukApiException(val code: Int = 0, override val message: String = UNKNOWN_ERROR) : Exception(message)

fun NunchukApiException.accountExisted() = code == ApiErrorCode.ACCOUNT_EXISTED

object ApiErrorCode {
    const val ACCOUNT_EXISTED = -100
    const val UNAUTHORIZED = 401
}

const val UNKNOWN_ERROR = "Unknown error"


