package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class ByzantinePreferenceSetup {
    SINGLE_PERSON, DISTRIBUTED
}

fun String.toByzantinePreferenceSetup(): ByzantinePreferenceSetup = ByzantinePreferenceSetup.values().find { it.name == this } ?: ByzantinePreferenceSetup.SINGLE_PERSON