package com.nunchuk.android.core.data.model.banner

import com.google.gson.annotations.SerializedName

internal class BannerListResponse {
    @SerializedName("reminder")
    val banner: BannerDto? = null
}


internal data class BannerDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("content") val content: BannerContentDto? = null
)

internal data class BannerContentDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
)