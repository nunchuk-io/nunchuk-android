package com.nunchuk.android.auth.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserTokenResponse(
    @SerializedName("token")
    val token: TokenResponse
) : Serializable

data class TokenResponse(
    @SerializedName("value")
    val value: String,
    @SerializedName("expireInSeconds")
    val expireInSeconds: Long = 0
) : Serializable
