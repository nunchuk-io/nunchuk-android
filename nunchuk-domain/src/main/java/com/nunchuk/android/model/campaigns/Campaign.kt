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
    val dismissible: Boolean,
    val isDismissed: Boolean
) : Parcelable {
    companion object {
        fun empty() = Campaign("", "", "", "", "", false, false)
    }

    fun isValid() = id.isNotEmpty()
}