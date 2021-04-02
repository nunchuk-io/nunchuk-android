package com.nunchuk.android.network

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
            if (_error != null) {
                throw NunchukApiException(_error.code, _error.message)
            }
            throw NunchukApiException()
        }
}

data class ErrorResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String
) : Serializable

class NunchukApiException(val code: Int = 0, override val message: String = "Unknown error") : Exception(message)

fun NunchukApiException.accountExisted() = code == -100


