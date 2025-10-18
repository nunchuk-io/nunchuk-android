package com.nunchuk.android.model.inheritance

data class InheritanceNotificationSettings(
    val emailMeWalletConfig: Boolean = true,
    val perEmailSettings: List<EmailNotificationSettings> = emptyList()
)