package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

data class ReferrerCodeResponseData(
    @SerializedName("code") val data: ReferrerCodeResponse?
)

data class ReferrerCodeResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("receive_address") val receiveAddress: String?,
    @SerializedName("receive_address_hash") val receiveAddressHash: String?,
    @SerializedName("campaign") val campaign: CampaignResponse?,
    @SerializedName("text_template") val textTemplate: String?,
)