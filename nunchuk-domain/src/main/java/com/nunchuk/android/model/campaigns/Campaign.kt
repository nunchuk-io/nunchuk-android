package com.nunchuk.android.model.campaigns

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Campaign(
    val id: String,
    val referrerTitle: String,
    val referrerDescriptionHtml: String,
    val referrerBannerUrl: String,
    val cta: String,
) : Parcelable {
    companion object {
        fun empty() = Campaign("", "", "", "", "")
    }

    fun isValid() = id.isNotEmpty()
}