package com.nunchuk.android.model.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerTransaction(
    val type: String = "",
    val broadcastTimeInMilis: Long = 0L,
    val spendingLimitMessage: String = "",
) : Parcelable