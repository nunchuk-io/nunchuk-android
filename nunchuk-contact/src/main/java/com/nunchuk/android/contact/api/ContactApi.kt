package com.nunchuk.android.contact.api

import com.nunchuk.android.core.network.Data
import retrofit2.http.*

internal interface ContactApi {

    @PUT("user/contacts/request")
    suspend fun addContacts(@Body payLoad: AddContactPayload): Data<AddContactsResponse>

    @GET("user/contacts/")
    suspend fun getContacts(): Data<ContactResponseWrapper>

    @HTTP(method = "DELETE", path = "user/contacts/request", hasBody = true)
    suspend fun cancelRequest(@Body payLoad: CancelRequestPayload)

    @POST("user/contacts/accept")
    suspend fun acceptContact(@Body payload: AcceptRequestPayload)

    @GET("user/contacts/request/sent")
    suspend fun getPendingSentContacts(): Data<UsersResponseWrapper>

    @GET("user/contacts/request/received")
    suspend fun getPendingApprovalContacts(): Data<UsersResponseWrapper>

    @GET("user/search")
    suspend fun searchContact(@Query("email") email: String): Data<UserResponseWrapper>

    @POST("user/auto-complete-search")
    suspend fun autoCompleteSearch(@Body payload: AutoCompleteSearchContactPayload): Data<ContactResponseWrapper>

    @PUT("user/me")
    suspend fun updateContact(@Body payload: UpdateContactPayload): Data<UserResponseWrapper>

    @POST("passport/invite")
    suspend fun invite(@Body payload: InviteContactPayload): Data<Unit>

}