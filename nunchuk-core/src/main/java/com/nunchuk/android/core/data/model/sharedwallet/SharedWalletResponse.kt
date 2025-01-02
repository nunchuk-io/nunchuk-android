package com.nunchuk.android.core.data.model.sharedwallet

import com.google.gson.annotations.SerializedName

data class GroupWalletResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("status")
    val status: String
)