package com.nunchuk.android.model.setting

import com.google.gson.annotations.SerializedName

data class TaprootFeeSelectionSetting(
    @SerializedName("fee_difference_threshold_percent")
    val feeDifferenceThresholdPercent: Int = 10,
    @SerializedName("fee_difference_threshold_currency")
    val feeDifferenceThresholdCurrency: Double = 0.2,
    @SerializedName("automatic_fee_enabled")
    val automaticFeeEnabled: Boolean = false,
    @SerializedName("is_first_time")
    val isFirstTime: Boolean = true,
)