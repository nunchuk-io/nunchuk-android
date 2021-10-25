package com.nunchuk.android.core.profile

import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserProfileApi {

    @GET("user/me")
    suspend fun getUserProfile(): Data<UserResponseWrapper>

    @PUT("user/me")
    suspend fun updateUserProfile(@Body updatePayload: UpdateUserProfilePayload): Data<UserResponseWrapper>

}