package com.nunchuk.android.model.transaction

data class AlertPayload(
    val masterName: String,
    val pendingKeysCount: Int
)