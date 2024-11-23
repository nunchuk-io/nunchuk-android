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
    @SerializedName("metadata") val metaData: ReferrerMetaDataResponse?,
    @SerializedName("status") val status: String?,
)

data class ReferrerMetaDataResponse(
    @SerializedName("redirect_url") val redirectUrl: String?,
    @SerializedName("current_downloaded") val currentDownloaded: Int?,
    @SerializedName("required_downloaded") val requiredDownloaded: Int?,
)