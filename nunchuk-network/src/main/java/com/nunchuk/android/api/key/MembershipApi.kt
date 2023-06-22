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

package com.nunchuk.android.api.key

import com.nunchuk.android.core.network.Data
import com.nunchuk.android.model.MembershipSubscription
import com.nunchuk.android.model.VerifiedPKeyTokenRequest
import com.nunchuk.android.model.VerifiedPasswordTokenRequest
import com.nunchuk.android.model.VerifiedPasswordTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MembershipApi {
    @GET("/v1.1/subscriptions/current")
    suspend fun getCurrentSubscription(): Data<MembershipSubscription>

    @GET("/v1.1/subscriptions/testnet")
    suspend fun getTestnetCurrentSubscription(): Data<MembershipSubscription>

    @POST("/v1.1/passport/verified-password-token/{target_action}")
    suspend fun verifiedPasswordToken(
        @Path("target_action") targetAction: String,
        @Body payload: VerifiedPasswordTokenRequest
    ): Data<VerifiedPasswordTokenResponse>

    @POST("/v1.1/passport/verified-pkey-token/{target_action}")
    suspend fun verifiedPKeyToken(
        @Path("target_action") targetAction: String,
        @Body payload: VerifiedPKeyTokenRequest
    ): Data<VerifiedPasswordTokenResponse>
}