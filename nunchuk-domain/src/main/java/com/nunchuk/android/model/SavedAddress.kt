package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SavedAddress(
    val address: String,
    val label: String
) : Parcelable