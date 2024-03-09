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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.BannerApi
import com.nunchuk.android.core.data.model.banner.SubmitEmailViewAssistedWalletRequest
import com.nunchuk.android.core.data.model.onboarding.SendOnboardNoAdvisorRequest
import com.nunchuk.android.model.Country
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.model.banner.BannerPageItem
import com.nunchuk.android.repository.BannerRepository
import javax.inject.Inject

internal class BannerRepositoryImpl @Inject constructor(
    private val api: BannerApi,
) : BannerRepository {
    override suspend fun submitEmail(reminderId: String?, email: String) {
        api.submitEmail(
            SubmitEmailViewAssistedWalletRequest(
                email = email,
                reminderId = reminderId,
            )
        )
    }

    override suspend fun getAssistedWalletContent(reminderId: String): BannerPage {
        val response = api.getAssistedWalletBannerContent(reminderId)
        val page = response.data.page
        return BannerPage(
            title = page?.content?.title.orEmpty(),
            desc = page?.content?.description.orEmpty(),
            items = page?.content?.items.orEmpty().map {
                BannerPageItem(
                    it.title.orEmpty(),
                    it.description.orEmpty(),
                    it.iconUrl.orEmpty()
                )
            }
        )
    }

    override suspend fun getBanners(): Banner? {
        val response = api.getBanners()
        val banner = response.data.banner ?: return null
        return Banner(
            id = banner.id.orEmpty(),
            desc = banner.content?.description.orEmpty(),
            title = banner.content?.title.orEmpty(),
            url = banner.content?.imageUrl.orEmpty()
        )
    }

    override suspend fun getCountries(): List<Country> {
        val response = api.getOnboardingCountries()
        return response.data.countries.map {
            Country(
                name = it.name,
                code = it.code
            )
        }
    }

    override suspend fun sendOnboardingNoAdvisor(email: String, countryCode: String, note: String?) {
        val response = api.sendOnboardingNoAdvisor(SendOnboardNoAdvisorRequest(
            email = email,
            countryCode = countryCode,
            note = note
        ))
        if (response.isSuccess.not()) throw response.error
    }
}