package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmailNotificationSettings(
    val email: String,
    val notifyOnTimelockExpiry: Boolean = true,
    val notifyOnWalletChanges: Boolean = true,
    val includeWalletConfiguration: Boolean = true,
    /**
     * On-chain only: zero-based fallback indexes this recipient gets unlock notices for. Null is
     * "not sent", which keeps the server's selection on update; empty turns fallback notices off.
     */
    val notifyFallbacks: List<Int>? = null,
): Parcelable