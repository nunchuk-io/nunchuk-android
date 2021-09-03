package com.nunchuk.android.auth.api

import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
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

    @GET("user/me")
    suspend fun me(): Data<UserResponseWrapper>

}