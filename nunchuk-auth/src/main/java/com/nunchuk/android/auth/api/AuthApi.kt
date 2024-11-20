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

import com.nunchuk.android.auth.domain.model.EmailAvailability
import com.nunchuk.android.auth.api.biometric.BiometricChallengeRequest
import com.nunchuk.android.auth.api.biometric.BiometricChallengeResponse
import com.nunchuk.android.auth.api.biometric.BiometricRegisterPublicKey
import com.nunchuk.android.auth.api.biometric.BiometricSignInRequest
import com.nunchuk.android.auth.api.biometric.BiometricVerifyChallengeRequest
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @POST("passport/sign-in/resend-verify-new-device-code")
    suspend fun resendVerifyNewDeviceCode(
        @Body verifyPayload: ResendVerifyNewDeviceCodePayload
    ): Data<Unit>

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

    @GET("/v1.1/passport/username-availability")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String,
        @Query("type") type: String,
    ): Data<EmailAvailability>

    @POST("/v1.1/passport/resend-password")
    suspend fun resendPassword(
        @Body payload: ResendPasswordRequest
    ): Data<Unit>

    @POST("/v1.1/passport/biometric/challenge")
    suspend fun getBiometricChallenge(@Body request: BiometricChallengeRequest): Data<BiometricChallengeResponse>

    @POST("/v1.1/passport/biometric/register-public-key")
    suspend fun biometricRegisterPublicKey(@Body request: BiometricRegisterPublicKey): Data<Unit>

    @POST("/v1.1/passport/biometric/verify-challenge")
    suspend fun biometricVerifyChallenge(@Body request: BiometricVerifyChallengeRequest): Data<UserTokenResponse>

    @POST("/v1.1/passport/biometric/signin")
    suspend fun biometricSignIn(@Body request: BiometricSignInRequest): Data<UserTokenResponse>
}