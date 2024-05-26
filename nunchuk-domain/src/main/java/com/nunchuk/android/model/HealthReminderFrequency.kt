package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class HealthReminderFrequency {
    NONE,
    MONTHLY,
    THREE_MONTHLY,
    SIX_MONTHLY,
    YEARLY
}

fun HealthReminderFrequency.isNone(): Boolean {
    return this == HealthReminderFrequency.NONE
}

fun String.toHealthReminderFrequency(): HealthReminderFrequency {
    return when (this) {
        "MONTHLY" -> HealthReminderFrequency.MONTHLY
        "THREE_MONTHLY" -> HealthReminderFrequency.THREE_MONTHLY
        "SIX_MONTHLY" -> HealthReminderFrequency.SIX_MONTHLY
        "YEARLY" -> HealthReminderFrequency.YEARLY
        else -> {
            HealthReminderFrequency.NONE
        }
    }
}

fun HealthReminderFrequency.toReadableString(): String {
    return when (this) {
        HealthReminderFrequency.MONTHLY -> "month"
        HealthReminderFrequency.THREE_MONTHLY -> "3 months"
        HealthReminderFrequency.SIX_MONTHLY -> "6 months"
        HealthReminderFrequency.YEARLY -> "year"
        else -> {
            ""
        }
    }
}