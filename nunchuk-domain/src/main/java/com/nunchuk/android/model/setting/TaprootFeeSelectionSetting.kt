package com.nunchuk.android.model.setting

import com.google.gson.annotations.SerializedName

data class TaprootFeeSelectionSetting(
    @SerializedName("fee_difference_threshold_percent")
    val feeDifferenceThresholdPercent: Int = 0,
    @SerializedName("fee_difference_threshold_usd")
    val feeDifferenceThresholdUsd: Float = 0f,
    @SerializedName("automatic_fee_enabled")
    val automaticFeeEnabled: Boolean = false,
) {
    fun isFirstTime(): Boolean {
        return feeDifferenceThresholdPercent == 0 && feeDifferenceThresholdUsd == 0f
    }
}