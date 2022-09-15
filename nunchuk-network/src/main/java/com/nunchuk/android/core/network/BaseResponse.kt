/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
                if (code != CODE_SUCCESS) {
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

    val isSuccess: Boolean
        get() = _error == null || _error.code == CODE_SUCCESS

    val error: NunchukApiException
    get() {
        if (_error != null && _error.code != 0) {
            return NunchukApiException(
                code = _error.code,
                message = _error.message ?: UNKNOWN_ERROR,
                errorDetail = _error.details
            )
        }
        return NunchukApiException()
    }
}

data class ErrorResponse(
    @SerializedName("code")
    val code: Int = -1,
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
const val CODE_SUCCESS = 0


