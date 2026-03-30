package com.nunchuk.android.core.data.model.sharedwallet

import com.google.gson.annotations.SerializedName

data class GroupWalletResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("status")
    val status: String,
)

data class InvitationListResponse(
    @SerializedName("invitations")
    val invitations: List<InvitationDto>? = null,
)

data class InvitationDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("inviter_name")
    val inviterName: String? = null,
    @SerializedName("inviter_email")
    val inviterEmail: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
)

data class GroupInvitationListResponse(
    @SerializedName("invitations")
    val invitations: List<GroupInvitationDto>? = null,
)

data class GroupInvitationDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("recipient_email")
    val recipientEmail: String? = null,
    @SerializedName("recipient_user_id")
    val recipientUserId: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("created_time")
    val createdTime: Long = 0L,
)