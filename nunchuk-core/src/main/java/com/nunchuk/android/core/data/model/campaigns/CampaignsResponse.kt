package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

data class CampaignsResponseData(
    @SerializedName("campaign") val data: CampaignResponse?
)

data class CampaignResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("referrer_title") val referrerTitle: String?,
    @SerializedName("referrer_description_html") val referrerDescriptionHtml: String?,
    @SerializedName("referrer_banner_url") val referrerBannerUrl: String?,
    @SerializedName("cta") val cta: String?,
)