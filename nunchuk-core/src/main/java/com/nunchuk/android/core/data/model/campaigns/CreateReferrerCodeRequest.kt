package com.nunchuk.android.core.data.model.campaigns

import com.google.gson.annotations.SerializedName

class CreateReferrerCodeRequest(
    @SerializedName("receive_address") val receiveAddress: String?,
    @SerializedName("email") val email: String
)