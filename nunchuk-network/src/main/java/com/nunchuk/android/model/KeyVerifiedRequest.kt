package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

data class KeyVerifiedRequest(
    @SerializedName("key_checksum")
    val keyCheckSum: String,
)