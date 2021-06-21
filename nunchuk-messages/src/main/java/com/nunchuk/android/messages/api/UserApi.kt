package com.nunchuk.android.messages.api

import com.nunchuk.android.network.Data
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface UserApi {

    @POST("user/friends/request")
    suspend fun addContacts(@Body payLoad: AddContactPayload): Data<ResponseBody>

    @GET("user/friends")
    suspend fun getContacts(): Data<List<UserResponse>>

    @GET("user/friends/request/sent")
    suspend fun getPendingSentContacts(): Data<List<UserResponse>>

    @GET("user/friends/request/received")
    suspend fun getPendingApprovalContacts(): Data<List<UserResponse>>

    @GET("user/search")
    suspend fun searchContact(@Query("email") email: String): Data<UserResponseWrapper>

    @POST("user/auto-complete-search")
    suspend fun autoCompleteSearch(payload: AutoCompleteSearchContactPayload): Data<UsersResponseWrapper>

}