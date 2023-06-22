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

import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("passport/register")
    suspend fun register(
        @Body registerPayload: RegisterPayload
    ): Data<UserTokenResponse>

    @POST("passport/sign-in")
    suspend fun signIn(
        @Body signInPayload: SignInPayload
    ): Data<UserTokenResponse>

    @POST("passport/sign-in/verify-new-device")
    suspend fun verifyNewDevice(
        @Body verifyPayload: VerifyNewDevicePayload
    ): Data<UserTokenResponse>

    @POST("passport/recover-password")
    suspend fun recoverPassword(
        @Body recoverPasswordPayload: RecoverPasswordPayload
    ): Data<Unit>

    @POST("passport/change-password")
    suspend fun changePassword(
        @Body changePasswordPayload: ChangePasswordPayload
    ): Data<Unit>

    @POST("passport/forgot-password")
    suspend fun forgotPassword(
        @Body forgotPasswordPayload: ForgotPasswordPayload
    ): Data<Unit>

    @POST("/v1.1/passport/qr/try-login")
    suspend fun tryLogin(
        @Body payload: TryLoginRequest
    ): Data<TryLoginResponse>

    @POST("/v1.1/passport/qr/confirm-login")
    suspend fun confirmLogin(
        @Body payload: ConfirmQrLoginRequest
    ): Data<Unit>
}