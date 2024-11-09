package com.nunchuk.android.model.campaigns

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReferrerMetaData(
    val redirectUrl: String,
    val currentDownloaded: Int,
    val requiredDownloaded: Int,
) : Parcelable