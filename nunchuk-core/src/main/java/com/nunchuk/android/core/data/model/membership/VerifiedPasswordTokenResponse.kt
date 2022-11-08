package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal class VerifiedPasswordTokenResponse(
    @SerializedName("token")
    val token: Data
) {
    class Data(
        @SerializedName("token") val token: String? = null,
        @SerializedName("user_id") val userId: String? = null,
        @SerializedName("expires_at") val expiresAt: Long? = null,
        @SerializedName("target_action") val targetAction: String? = null,
    )
}