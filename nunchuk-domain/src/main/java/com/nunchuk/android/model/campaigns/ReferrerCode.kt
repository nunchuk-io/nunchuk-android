package com.nunchuk.android.model.campaigns

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReferrerCode(
    val code: String,
    val link: String,
    val receiveAddress: String,
    val receiveAddressHash: String,
    val textTemplate: String = "",
    val campaign: Campaign,
    val localWalletId: String = "",
    val email: String = ""
) : Parcelable