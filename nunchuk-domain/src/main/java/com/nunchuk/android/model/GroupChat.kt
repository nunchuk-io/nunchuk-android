package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupChat(
    val createdTimeMillis: Long,
    val groupId: String,
    val historyPeriod: HistoryPeriod,
    val roomId: String
) : Parcelable