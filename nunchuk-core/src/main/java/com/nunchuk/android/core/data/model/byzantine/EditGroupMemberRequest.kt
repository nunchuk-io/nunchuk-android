package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName

data class EditGroupMemberRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Body? = null
) {
    data class Body(
        @SerializedName("members")
        val members: List<MemberRequest>? = null
    )
}