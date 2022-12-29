package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class VerifiedPasswordTokenRequest(
    @SerializedName("password")
    val password: String? = null
)