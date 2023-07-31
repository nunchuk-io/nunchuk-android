package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class ConfirmationCodeRequest(
    @SerializedName("nonce")
    val nonce: String? = null,
    @SerializedName("body")
    val body: Any? = null
)

class ConfirmationCodeVerifyRequest(
    @SerializedName("code")
    val code: String? = null
)

class ConfirmationCodeResponse(
    @SerializedName("code_id")
    val codeId: String? = null
)

class ConfirmationCodeVerifyResponse(
    @SerializedName("token")
    val token: String? = null
)