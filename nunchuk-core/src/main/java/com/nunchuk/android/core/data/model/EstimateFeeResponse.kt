package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

data class EstimateFeeResponse(
    @SerializedName("fastestFee")
    val priorityRate: Int,
    @SerializedName("hourFee")
    val standardRate: Int,
    @SerializedName("minimumFee")
    val economicRate: Int,
)
