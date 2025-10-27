package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmailNotificationSettings(
    val email: String,
    val notifyOnTimelockExpiry: Boolean = true,
    val notifyOnWalletChanges: Boolean = true,
    val includeWalletConfiguration: Boolean = true
): Parcelable