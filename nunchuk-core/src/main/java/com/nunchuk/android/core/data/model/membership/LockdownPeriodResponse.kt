package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

data class LockdownPeriodResponse(
    @SerializedName("periods")
    val periods: List<Data>? = null
) {
    data class Data(
        @SerializedName("id")
        val id: String? = null,
        @SerializedName("interval")
        val interval: String? = null,
        @SerializedName("interval_count")
        val intervalCount: Int? = null,
        @SerializedName("enabled")
        val enabled: Boolean? = null,
        @SerializedName("display_name")
        val displayName: String? = null,
    )
}