package com.nunchuk.android.model.wallet

data class Invitation(
    val id: String,
    val inviterName: String,
    val inviterEmail: String,
    val groupId: String,
)

data class GroupInvitation(
    val id: String,
    val groupId: String,
    val recipientEmail: String,
    val recipientUserId: String,
    val status: String,
    val createdTime: Long,
)
