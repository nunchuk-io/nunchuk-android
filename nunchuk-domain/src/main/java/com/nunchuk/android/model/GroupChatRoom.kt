package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupChatRoom(
    val groupId: String,
    val roomId: String,
    val isMasterOrAdmin: Boolean,
) : Parcelable