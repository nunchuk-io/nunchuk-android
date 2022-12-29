package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class GetNonceResponse(
    @SerializedName("nonce")
    val nonce: NonceDto? = null
)

data class NonceDto(
    @SerializedName("nonce")
    val nonce: String?= null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("expires_at")
    val expiresAtInMillis: Long = 0L,
)