package com.nunchuk.android.model

data class GroupChat(
    val createdTimeMillis: Long,
    val groupId: String,
    val historyPeriod: HistoryPeriod,
    val roomId: String
)