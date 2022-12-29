package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class SignServerTransactionRequest(
    @SerializedName("psbt")
    val psbt: String
)