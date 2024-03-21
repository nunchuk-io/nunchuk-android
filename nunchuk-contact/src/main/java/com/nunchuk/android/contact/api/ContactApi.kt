/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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

    @HTTP(method = "DELETE", path = "user/contacts/", hasBody = true)
    suspend fun deleteContact(
        @Body payload: DeleteContactPayload,
    ): Data<Unit>

}