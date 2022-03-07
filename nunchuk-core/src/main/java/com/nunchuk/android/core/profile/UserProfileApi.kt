package com.nunchuk.android.core.profile

import com.nunchuk.android.core.network.Data
import retrofit2.http.*

interface UserProfileApi {

    @GET("user/me")
    suspend fun getUserProfile(): Data<UserResponseWrapper>

    @POST("user/me/delete-confirmation")
    suspend fun confirmDeleteAccount(@Body payload: DeleteConfirmationPayload): Data<Unit>

    @DELETE("user/me")
    suspend fun requestDeleteAccount(): Data<Unit>

    @DELETE("passport/log-out")
    suspend fun signOut(): Data<Unit>

    @PUT("user/me")
    suspend fun updateUserProfile(@Body updatePayload: UpdateUserProfilePayload): Data<UserResponseWrapper>

}