package com.nunchuk.android.auth.api

import com.nunchuk.android.network.Data
import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Query

private const val QUERY_EMAIL = "email"
private const val QUERY_NAME = "name"
private const val QUERY_PASSWORD = "password"
private const val QUERY_OLD_PASSWORD = "oldPwd"
private const val QUERY_NEW_PASSWORD = "newPassword"
private const val QUERY_FORGOT_TOKEN = "forgotPwdToken"
private const val QUERY_CONFIRM_PASSWORD = "newPasswordConfirmed"

interface AuthApi {

    @POST("register")
    suspend fun register(
        @Query(QUERY_NAME) name: String,
        @Query(QUERY_EMAIL) email: String
    ): Data<UserTokenResponse>

    @POST("sign-in")
    suspend fun signIn(
        @Query(QUERY_EMAIL) email: String,
        @Query(QUERY_PASSWORD) password: String
    ): Data<UserTokenResponse>

    @POST("recover-password")
    suspend fun recoverPassword(
        @Query(QUERY_EMAIL) email: String,
        @Query(QUERY_FORGOT_TOKEN) forgotPwdToken: String,
        @Query(QUERY_NEW_PASSWORD) newPassword: String,
        @Query(QUERY_CONFIRM_PASSWORD) newPasswordConfirmed: String
    ): ResponseBody

    @POST("change-password")
    suspend fun changePassword(
        @Query(QUERY_OLD_PASSWORD) oldPwd: String,
        @Query(QUERY_NEW_PASSWORD) newPassword: String,
        @Query(QUERY_CONFIRM_PASSWORD) newPasswordConfirmed: String
    ): ResponseBody

    @POST("forgot-password")
    suspend fun forgotPassword(
        @Query(QUERY_EMAIL) email: String
    ): ResponseBody


}