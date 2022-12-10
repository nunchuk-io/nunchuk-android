package com.nunchuk.android.transaction.components.schedule.timezone

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.OffsetDateTime
import java.time.ZoneId

@Parcelize
data class TimeZoneDetail(
    val id: String = "",
    val city: String = "",
    val country: String = "",
    val offset: String = "",
) : Parcelable

fun String.toTimeZoneDetail(): TimeZoneDetail? {
    val zone = ZoneId.of(this)
    val offsetToday = OffsetDateTime.now(zone).offset
    if (offsetToday.id == "Z") return null

    val tokens = this.replace("_", " ").split("/")
    if (tokens.size != 2) return null
    return TimeZoneDetail(
        id = this,
        country = tokens[0],
        city = tokens[1],
        offset = "GMT${offsetToday.id}"
    )
}