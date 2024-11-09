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
    val isDismissed: Boolean,
    val type: CampaignType
) : Parcelable {
    companion object {
        fun empty() = Campaign(
            "", "", "", "", "",
            dismissible = false,
            isDismissed = false,
            type = CampaignType.GENERAL
        )
    }

    fun isValid() = id.isNotEmpty()
}