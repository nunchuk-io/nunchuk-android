package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

data class SendConfirmationCodeByEmailResponse(
    @SerializedName("code_id") val codeId: String
)