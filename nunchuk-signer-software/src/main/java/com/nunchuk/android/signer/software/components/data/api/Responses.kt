package com.nunchuk.android.signer.software.components.data.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PKeyNonceResponse(
    @SerializedName("nonce")
    val nonce: String,
) : Serializable