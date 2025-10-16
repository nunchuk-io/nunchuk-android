package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

data class EmailNotificationSettings(
    val email: String,
    val notifyOnTimelockExpiry: Boolean = true,
    val notifyOnWalletChanges: Boolean = true,
    val includeWalletConfiguration: Boolean = true
)

