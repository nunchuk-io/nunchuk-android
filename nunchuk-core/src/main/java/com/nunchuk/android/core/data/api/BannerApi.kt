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

import com.nunchuk.android.core.data.model.banner.AssistedContentResponse
import com.nunchuk.android.core.data.model.banner.BannerListResponse
import com.nunchuk.android.core.data.model.banner.SubmitEmailViewAssistedWalletRequest
import com.nunchuk.android.core.data.model.onboarding.CountryDataResponse
import com.nunchuk.android.core.data.model.onboarding.SendOnboardNoAdvisorRequest
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface BannerApi {
    @POST("/v1.1/banners/pages/submit-email")
    suspend fun submitEmail(@Body payload: SubmitEmailViewAssistedWalletRequest): Data<Unit>

    @GET("/v1.1/banners/pages/assisted-wallet")
    suspend fun getAssistedWalletBannerContent(@Query("reminder_id") requestId: String): Data<AssistedContentResponse>

    @GET("/v1.1/banners/reminders/home_v2")
    suspend fun getBanners(): Data<BannerListResponse>

    @GET("/v1.1/banners/onboarding/countries")
    suspend fun getOnboardingCountries(): Data<CountryDataResponse>

    @POST("/v1.1/banners/onboarding/no-advisor")
    suspend fun sendOnboardingNoAdvisor(@Body payload: SendOnboardNoAdvisorRequest): Data<Unit>
}