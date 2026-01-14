package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ClaimSigningChallenge(
    val id: String,
    val message: String
) : Parcelable