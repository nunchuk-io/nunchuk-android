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