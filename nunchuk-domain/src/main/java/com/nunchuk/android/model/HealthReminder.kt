package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HealthReminder(
    val xfp: String,
    val frequency: String,
    val startDateMillis: Long,
) : Parcelable