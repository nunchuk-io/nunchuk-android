package com.nunchuk.android.signer.software.components.data.api

import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.PKeySignInResponse
import com.nunchuk.android.model.PKeySignUpResponse
import retrofit2.http.*

internal interface SignerSoftwareApi {
    @GET("passport/pkey/nonce")
    suspend fun getPKeyNonce(
        @Query("address") address: String?,
        @Query("username") username: String
    ): Data<PKeyNonceResponse>

    @POST("passport/pkey/nonce")
    suspend fun postPKeyNonce(
        @Body payload: PKeyNoncePayload
    ): Data<PKeyNonceResponse>

    @POST("passport/pkey/nonce?type=change_pkey")
    suspend fun postPKeyNonceForReplace(
        @Body payload: PKeyNoncePayload
    ): Data<PKeyNonceResponse>

    @POST("passport/pkey/signup")
    suspend fun postPKeySignUp(
        @Body payload: PKeySignUpPayload
    ): Data<PKeySignUpResponse>

    @POST("passport/pkey/signin")
    suspend fun postPKeySignIn(
        @Body payload: PKeySignInPayload
    ): Data<PKeySignInResponse>

    @GET("user/pkey/{public_address}")
    suspend fun getUserInfoPKey(
        @Path("public_address") address: String
    ): Data<UserResponseWrapper>

    @GET("passport/username-availability")
    suspend fun checkUsername(
        @Query("username") username: String
    ): Data<UserResponseWrapper>

    @POST("user/pkey/change-pkey")
    suspend fun changePKey(
        @Body payload: PKeyChangeKeyPayload
    )

    @POST("user/pkey/delete-confirmation")
    suspend fun deletePKey(@Body payload: PKeyDeleteKeyPayload)
}