/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RegisterPayload(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
) : Serializable

data class SignInPayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
) : Serializable

data class RecoverPasswordPayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("forgotPasswordToken")
    val forgotPasswordToken: String,
    @SerializedName("newPassword")
    val newPassword: String
) : Serializable

data class ChangePasswordPayload(
    @SerializedName("oldPassword")
    val oldPassword: String,
    @SerializedName("newPassword")
    val newPassword: String
) : Serializable

data class ForgotPasswordPayload(
    @SerializedName("email")
    val oldPassword: String
) : Serializable

data class VerifyNewDevicePayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("loginHalfToken")
    val loginHalfToken: String,
    @SerializedName("pin")
    val pin: String,
    @SerializedName("deviceId")
    val deviceId: String
): Serializable

data class TryLoginRequest(
    @SerializedName("uuid")
    val uuid: String?,
    @SerializedName("qr_payload")
    val qrCode: String
)

data class ConfirmQrLoginRequest(
    @SerializedName("uuid")
    val uuid: String?,
    @SerializedName("token")
    val token: String
)

data class ResendVerifyNewDeviceCodePayload(
    @SerializedName("email")
    val email: String,
    @SerializedName("loginHalfToken")
    val loginHalfToken: String,
    @SerializedName("deviceId")
    val deviceId: String
): Serializable

data class ResendPasswordRequest(
    @SerializedName("email")
    val email: String
)

data class GoogleSignInPayload(
    @SerializedName("id_token")
    val idToken: String
) : Serializable