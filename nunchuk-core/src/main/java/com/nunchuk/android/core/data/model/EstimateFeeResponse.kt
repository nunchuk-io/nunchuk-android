package com.nunchuk.android.core.data.model

import com.google.gson.annotations.SerializedName

data class EstimateFeeResponse(
    @SerializedName("fastestFee")
    val priorityRate: Int,
    @SerializedName("halfHourFee")
    val standardRate: Int,
    @SerializedName("hourFee")
    val economicRate: Int,
    @SerializedName("minimumFee")
    val minimumFee: Int,
)
