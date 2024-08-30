package com.nunchuk.android.app.referral

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConfirmationCodeResultData(
    val address: String?,
    val action: String,
    val token: String,
    val walletId: String? = null
) : Parcelable {
    companion object {
        val empty = ConfirmationCodeResultData(null, "", "")
    }
}