package com.nunchuk.android.core.data.model.banner

import com.google.gson.annotations.SerializedName

internal class AssistedContentResponse {
    @SerializedName("page")
    val page: Page? = null
}

internal data class Content(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("items") val items: ArrayList<Items> = arrayListOf()
)

internal data class Items(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("icon_url") val iconUrl: String? = null,
)

internal data class Page(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("content") val content: Content? = null,
)