package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.model.HealthReminder

data class HealthReminderResponse(
    @SerializedName("reminders")
    val reminders: List<ReminderResponse>?,
)

data class ReminderResponse(
    @SerializedName("xfp")
    val xfp: String?,
    @SerializedName("frequency")
    val frequency: String?,
    @SerializedName("start_date_millis")
    val startDateMillis: Long?,
)

data class HealthReminderRequest(
    @SerializedName("xfps") val xfps: List<String>,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("start_date_millis") val startDateMillis: Long,
)

fun ReminderResponse.toHealthReminder(): HealthReminder {
    return HealthReminder(
        xfp = xfp ?: "",
        frequency = frequency ?: "",
        startDateMillis = startDateMillis ?: 0
    )
}