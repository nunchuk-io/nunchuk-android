package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class VerifySecurityQuestionResponse(
    @SerializedName("token")
    val token: VerifySecurityQuestionDto? = null
)

data class VerifySecurityQuestionDto(
    @SerializedName("token")
    val token: String? = null,
)