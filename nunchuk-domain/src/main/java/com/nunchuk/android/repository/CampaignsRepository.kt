package com.nunchuk.android.repository

import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode

interface CampaignsRepository {
    suspend fun getCurrentCampaigns(email: String?): Campaign?

    suspend fun getReferrerCodesByEmail(email: String, token: String?): ReferrerCode?

    suspend fun createReferrerCodeByEmail(
        receiveAddress: String,
        email: String,
        walletId: String
    ): ReferrerCode?

    suspend fun updateReceiveAddressByEmail(
        receiveAddress: String,
        email: String,
        token: String,
        walletId: String
    ): ReferrerCode?

    suspend fun verifyConfirmationCodeByEmail(
        codeId: String,
        code: String,
        email: String
    ): String

    suspend fun sendConfirmationCodeByEmail(
        action: String,
        email: String,
        receiveAddress: String?
    ): String

    suspend fun dismissCampaign(email: String, campaign: Campaign)
}