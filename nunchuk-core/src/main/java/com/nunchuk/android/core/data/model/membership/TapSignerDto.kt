package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class TapSignerDto(
    @SerializedName("card_id")
    val cardId: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("is_testnet")
    val isTestnet: Boolean,
    @SerializedName("birth_height")
    val birthHeight: Int,
)