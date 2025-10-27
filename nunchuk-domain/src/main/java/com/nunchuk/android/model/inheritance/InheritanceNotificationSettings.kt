package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InheritanceNotificationSettings(
    val emailMeWalletConfig: Boolean = true,
    val perEmailSettings: List<EmailNotificationSettings> = emptyList()
): Parcelable