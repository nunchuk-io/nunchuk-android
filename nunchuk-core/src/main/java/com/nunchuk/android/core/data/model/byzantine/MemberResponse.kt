package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.UserResponse

data class MemberResponse(
    @SerializedName("email_or_username")
    val emailOrUsername: String? = null,
    @SerializedName("membership_id")
    val membershipId: String? = null,
    @SerializedName("permissions")
    val permissions: List<String>? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("inviter_user_id")
    val inviterUserId: String? = null,
    @SerializedName("user")
    val user: UserResponse? = null
)