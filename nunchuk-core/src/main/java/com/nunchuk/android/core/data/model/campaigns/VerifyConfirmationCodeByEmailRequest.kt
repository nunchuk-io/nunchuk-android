package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

data class VerifyConfirmationCodeByEmailRequest(
    @SerializedName("code") val code: String,
    @SerializedName("email") val email: String
)