package com.nunchuk.android.model.inheritance

data class EmailNotificationSettings(
    val email: String,
    val notifyOnTimelockExpiry: Boolean = true,
    val notifyOnWalletChanges: Boolean = true,
    val includeWalletConfiguration: Boolean = true
)