package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

data class CreateDraftGroupWalletRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("primary_membership_id")
    val primaryMembershipId: String?,
)