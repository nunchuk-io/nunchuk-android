package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HistoryPeriod(
    val displayName: String = "",
    val enabled: Boolean = false,
    val id: String = "",
    val interval: String = "",
    val intervalCount: Int = 0
) : Parcelable