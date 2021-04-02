package com.nunchuk.android.auth.api

import com.nunchuk.android.network.Data
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("register")
    suspend fun register(
        @Body registerPayload: RegisterPayload
    ): Data<UserTokenResponse>

    @POST("sign-in")
    suspend fun signIn(
        @Body signInPayload: SignInPayload
    ): Data<UserTokenResponse>

    @POST("recover-password")
    suspend fun recoverPassword(
        @Body recoverPasswordPayload: RecoverPasswordPayload
    ): ResponseBody

    @POST("change-password")
    suspend fun changePassword(
        @Body changePasswordPayload: ChangePasswordPayload
    ): ResponseBody

    @POST("forgot-password")
    suspend fun forgotPassword(
        @Body forgotPasswordPayload: ForgotPasswordPayload
    ): ResponseBody

}