package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.CreateServerKeysPayload

internal class KeyPolicyUpdateRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("iat")
    val iat: Long? = null,
    @SerializedName("exp")
    val exp: Long? = null,
    @SerializedName("body")
    val body: CreateServerKeysPayload? = null
)