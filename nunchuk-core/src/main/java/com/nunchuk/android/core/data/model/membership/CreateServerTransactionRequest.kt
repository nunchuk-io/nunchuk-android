package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class CreateServerTransactionRequest(
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("psbt")
    val psbt: String
)