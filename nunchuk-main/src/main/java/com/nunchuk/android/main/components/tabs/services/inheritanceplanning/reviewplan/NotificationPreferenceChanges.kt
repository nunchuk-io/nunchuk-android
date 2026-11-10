package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.unlockNoticeRoutes
import com.nunchuk.android.model.inheritance.EmailNotificationSettings

/**
 * Which values of one recipient's notification settings a pending plan change alters (REV-11
 * "inheritance plan change"): only those are shown in the changed color, not the whole card.
 */
data class RecipientNotificationChanges(
    val unlockNotices: Boolean = false,
    val walletChanges: Boolean = false,
    val includeWalletConfiguration: Boolean = false,
) {
    companion object {
        /** A recipient the plan did not have before: everything about them is new. */
        val ALL = RecipientNotificationChanges(
            unlockNotices = true,
            walletChanges = true,
            includeWalletConfiguration = true,
        )
    }
}

/**
 * Compares this recipient with [previous] (the same email in the plan before the change). With
 * fallbacks the unlock notices compare as the set of timelocks picked; without, as the single
 * "notify when the timelock expires" flag.
 */
fun EmailNotificationSettings.changesFrom(
    previous: EmailNotificationSettings?,
    fallbackCount: Int,
): RecipientNotificationChanges {
    if (previous == null) return RecipientNotificationChanges.ALL
    val unlockNoticesChanged = if (fallbackCount > 0) {
        unlockNoticeRoutes(fallbackCount) != previous.unlockNoticeRoutes(fallbackCount)
    } else {
        notifyOnTimelockExpiry != previous.notifyOnTimelockExpiry
    }
    return RecipientNotificationChanges(
        unlockNotices = unlockNoticesChanged,
        walletChanges = notifyOnWalletChanges != previous.notifyOnWalletChanges,
        includeWalletConfiguration = includeWalletConfiguration != previous.includeWalletConfiguration,
    )
}
