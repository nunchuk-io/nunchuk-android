package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

class VerifyConfirmationCodeByEmailResponse(
    @SerializedName("token") val token: String
)