package com.nunchuk.android.core.data.model.banner

import com.google.gson.annotations.SerializedName

data class SubmitEmailViewAssistedWalletRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("reminder_id")
    val reminderId: String,
)