package com.nunchuk.android.messages.api

import com.nunchuk.android.network.Data
import okhttp3.ResponseBody
import retrofit2.http.*

internal interface ContactApi {

    @PUT("user/contacts/request")
    suspend fun addContacts(@Body payLoad: AddContactPayload): Data<ResponseBody>

    @GET("user/contacts/")
    suspend fun getContacts(): Data<List<UserResponse>>

    @GET("user/contacts/request/sent")
    suspend fun getPendingSentContacts(): Data<List<UserResponse>>

    @GET("user/contacts/request/received")
    suspend fun getPendingApprovalContacts(): Data<List<UserResponse>>

    @GET("user/search")
    suspend fun searchContact(@Query("email") email: String): Data<UserResponseWrapper>

    @POST("user/auto-complete-search")
    suspend fun autoCompleteSearch(payload: AutoCompleteSearchContactPayload): Data<UsersResponseWrapper>

}