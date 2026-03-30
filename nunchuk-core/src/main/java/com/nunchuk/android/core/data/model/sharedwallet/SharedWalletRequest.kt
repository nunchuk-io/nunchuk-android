package com.nunchuk.android.core.data.model.sharedwallet

import com.google.gson.annotations.SerializedName

data class JoinSharedWalletRequest(
    @SerializedName("group_id")
    val groupId: String,
)

data class CreateInvitationRequest(
    @SerializedName("group_id")
    val groupId: String,
    @SerializedName("emails")
    val emails: List<String>,
)