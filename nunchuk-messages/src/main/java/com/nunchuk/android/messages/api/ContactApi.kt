package com.nunchuk.android.messages.api

import com.nunchuk.android.network.Data
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.*

internal interface ContactApi {

    @PUT("user/contacts/request")
    fun addContacts(@Body payLoad: AddContactPayload): Single<Data<AddContactsResponse>>

    @GET("user/contacts/")
    fun getContacts(): Single<Data<UsersResponseWrapper>>

    @GET("user/contacts/request/sent")
    suspend fun getPendingSentContacts(): Data<UsersResponseWrapper>

    @GET("user/contacts/request/received")
    suspend fun getPendingApprovalContacts(): Data<UsersResponseWrapper>

    @GET("user/search")
    suspend fun searchContact(@Query("email") email: String): Data<UserResponseWrapper>

    @POST("user/auto-complete-search")
    suspend fun autoCompleteSearch(payload: AutoCompleteSearchContactPayload): Data<UsersResponseWrapper>

}