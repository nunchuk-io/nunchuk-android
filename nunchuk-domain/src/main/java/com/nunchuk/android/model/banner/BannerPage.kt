package com.nunchuk.android.model.banner

data class BannerPage(
    val title: String,
    val desc: String,
    val items: List<BannerPageItem> = emptyList(),
)

data class BannerPageItem(
    val title: String,
    val desc: String,
    val url: String,
)