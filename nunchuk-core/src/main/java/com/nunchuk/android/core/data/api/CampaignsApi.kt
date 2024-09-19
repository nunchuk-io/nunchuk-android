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

package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.campaigns.CampaignsResponseData
import com.nunchuk.android.core.data.model.campaigns.CreateReferrerCodeRequest
import com.nunchuk.android.core.data.model.campaigns.ReferrerCodeResponseData
import com.nunchuk.android.core.data.model.campaigns.SendConfirmationCodeByEmailResponse
import com.nunchuk.android.core.data.model.campaigns.VerifyConfirmationCodeByEmailRequest
import com.nunchuk.android.core.data.model.campaigns.VerifyConfirmationCodeByEmailResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface CampaignsApi {

    @GET("/v1.1/referral/campaigns/current")
    suspend fun getCurrentCampaigns(@Query("email") email: String?): Data<CampaignsResponseData>

    @GET("/v1.1/referral/referrer/codes/by-email")
    suspend fun getReferrerCodesByEmail(@HeaderMap headers: Map<String, String>, @Query("email") email: String): Data<ReferrerCodeResponseData>

    @POST("/v1.1/referral/referrer/codes/by-email")
    suspend fun createReferrerCodeByEmail(@Body request: CreateReferrerCodeRequest): Data<ReferrerCodeResponseData>

    @PUT("/v1.1/referral/referrer/codes/by-email")
    suspend fun updateReceiveAddressByEmail(
        @HeaderMap headers: Map<String, String>,
        @Body request: CreateReferrerCodeRequest
    ): Data<ReferrerCodeResponseData>

    @POST("/v1.1/referral/confirmation-code/by-email")
    suspend fun sendConfirmationCodeByEmail(
        @Query("action") action: String,
        @Body request: CreateReferrerCodeRequest
    ): Data<SendConfirmationCodeByEmailResponse>

    @POST("/v1.1/referral/confirmation-code/by-email/{code_id}/verify")
    suspend fun verifyConfirmationCodeByEmail(
        @Path("code_id") codeId: String,
        @Body request: VerifyConfirmationCodeByEmailRequest
    ): Data<VerifyConfirmationCodeByEmailResponse>

    @PUT("/v1.1/referral/campaigns/current/dismiss")
    suspend fun dismissCampaign(@Query("email") email: String): Data<Unit>
}