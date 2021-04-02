package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserTokenResponse(
    @SerializedName("tokenId")
    val tokenId: String,
    @SerializedName("expireInSeconds")
    val expireInSeconds: Long = 0
) : Serializable