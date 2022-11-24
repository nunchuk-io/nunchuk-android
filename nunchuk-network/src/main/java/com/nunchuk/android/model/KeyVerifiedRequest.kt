package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class KeyVerifiedRequest(
    @SerializedName("key_checksum")
    val keyCheckSum: String,
    @SerializedName("verification_type")
    val verificationType: String,
)