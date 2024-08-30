package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

data class RefereeCodeResponseData(
    @SerializedName("code") val data: RefereeCodeResponse
)

data class RefereeCodeResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("expiry_time_millis") val expiryTimeMillis: Long?,
    @SerializedName("campaign") val campaign: CampaignRefereeCodeResponse?,
    @SerializedName("target_packages") val targetPackages: List<TargetPackagesResponse>?,
)

data class TargetPackagesResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
)

data class CampaignRefereeCodeResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("referee_description_html") val refereeDescriptionHtml: String?,
    @SerializedName("referee_code_usage_html") val refereeCodeUsageHtml: String?,
    @SerializedName("referee_banner_url") val refereeBannerUrl: String?,
)