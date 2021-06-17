package com.nunchuk.android.messages.api

import com.nunchuk.android.network.Data
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApi {

    @POST("user/friends/request")
    suspend fun addContacts(@Body payLoad: AddContactPayload): Data<ResponseBody>

    @GET("user/friends/request")
    suspend fun getContacts(): Data<List<UserResponse>>
}