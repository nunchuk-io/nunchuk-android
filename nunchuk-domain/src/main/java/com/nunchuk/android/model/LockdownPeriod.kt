package com.nunchuk.android.model

data class LockdownPeriod(
    val id: String,
    val interval: String,
    val intervalCount: Int,
    val enabled: Boolean,
    val displayName: String
)