package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

internal data class CreateDraftWalletRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("primary_membership_id")
    val primaryMembershipId: String? = null,
    @SerializedName("send_bsms_email")
    val sendBsmsEmail: Boolean = false,
)