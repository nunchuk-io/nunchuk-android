package com.nunchuk.android.contact.api

import com.nunchuk.android.network.Data
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.*

internal interface ContactApi {

    @PUT("user/contacts/request")
    fun addContacts(@Body payLoad: AddContactPayload): Single<Data<AddContactsResponse>>

    @GET("user/contacts/")
    fun getContacts(): Single<Data<ContactResponseWrapper>>

    @HTTP(method = "DELETE", path = "user/contacts/request", hasBody = true)
    fun cancelRequest(@Body payLoad: CancelRequestPayload): Completable

    @POST("user/friends/accept")
    fun acceptContact(@Body payload: AcceptRequestPayload): Completable

    @GET("user/contacts/request/sent")
    fun getPendingSentContacts(): Single<Data<UsersResponseWrapper>>

    @GET("user/contacts/request/received")
    fun getPendingApprovalContacts(): Single<Data<UsersResponseWrapper>>

    @GET("user/search")
    suspend fun searchContact(@Query("email") email: String): Data<UserResponseWrapper>

    @POST("user/auto-complete-search")
    suspend fun autoCompleteSearch(payload: AutoCompleteSearchContactPayload): Data<ContactResponseWrapper>

}