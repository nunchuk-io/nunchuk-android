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

import android.util.Log
import com.google.gson.Gson
import com.nunchuk.android.core.data.api.CampaignsApi
import com.nunchuk.android.core.data.model.campaigns.CreateReferrerCodeRequest
import com.nunchuk.android.core.data.model.campaigns.VerifyConfirmationCodeByEmailRequest
import com.nunchuk.android.core.mapper.toModel
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.repository.CampaignsRepository
import javax.inject.Inject

internal class CampaignsRepositoryImpl @Inject constructor(
    private val api: CampaignsApi,
    private val ncDataStore: NcDataStore,
    private val gson: Gson
) : CampaignsRepository {

    override suspend fun getCurrentCampaigns(email: String?): Campaign? {
        val campaign = api.getCurrentCampaigns(email = email).data.data?.toModel()
        if (campaign != null) {
            ncDataStore.setCampaign(
                gson.toJson(campaign),
                email = email.orEmpty()
            )
        } else {
            ncDataStore.setCampaign(gson.toJson(Campaign.empty()), email = email.orEmpty())
        }
        return campaign
    }

    override suspend fun getReferrerCodesByEmail(email: String, token: String?): ReferrerCode? {
        val headers = if (token != null) mapOf("Confirmation-token" to "$token") else emptyMap()
        val referrerCode =
            api.getReferrerCodesByEmail(email = email, headers = headers).data.data ?: return null
        return referrerCode.toModel()
    }

    override suspend fun createReferrerCodeByEmail(
        receiveAddress: String,
        email: String,
        walletId: String
    ): ReferrerCode? {
        val response = api.createReferrerCodeByEmail(
            CreateReferrerCodeRequest(
                receiveAddress = receiveAddress,
                email = email
            )
        )
        var referrerCode = response.data.data?.toModel()
        if (referrerCode != null) {
            referrerCode = referrerCode.copy(localWalletId = walletId, email = email)
            ncDataStore.setReferrerCode(gson.toJson(referrerCode))
        }
        return referrerCode
    }

    override suspend fun updateReceiveAddressByEmail(
        receiveAddress: String,
        email: String,
        token: String,
        walletId: String
    ): ReferrerCode? {
        val response = api.updateReceiveAddressByEmail(
            headers = mapOf("Confirmation-token" to token),
            request = CreateReferrerCodeRequest(receiveAddress = receiveAddress, email = email)
        )
        var referrerCode = response.data.data?.toModel()
        if (referrerCode != null) {
            referrerCode = referrerCode.copy(email = email, localWalletId = walletId)
            ncDataStore.setReferrerCode(gson.toJson(referrerCode))
        }
        return referrerCode
    }

    override suspend fun verifyConfirmationCodeByEmail(
        codeId: String,
        code: String,
        email: String
    ): String {
        val response = api.verifyConfirmationCodeByEmail(
            codeId = codeId,
            request = VerifyConfirmationCodeByEmailRequest(code = code, email = email)
        )
        return response.data.token
    }

    override suspend fun sendConfirmationCodeByEmail(
        action: String,
        email: String,
        receiveAddress: String?
    ): String {
        val response = api.sendConfirmationCodeByEmail(
            action = action,
            request = CreateReferrerCodeRequest(receiveAddress = receiveAddress, email = email)
        )
        return response.data.codeId
    }

}