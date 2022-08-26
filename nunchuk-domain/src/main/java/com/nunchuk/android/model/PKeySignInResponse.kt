package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PKeySignInResponse(
    @SerializedName("tokenId")
    val tokenId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("expireInSeconds")
    val expireInSeconds: Long
) : Serializable