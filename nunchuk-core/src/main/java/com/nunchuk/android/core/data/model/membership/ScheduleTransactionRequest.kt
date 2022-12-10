package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class ScheduleTransactionRequest(
    @SerializedName("schedule_time_milis")
    val scheduleTime: Long,
    @SerializedName("psbt")
    val psbt: String,
)