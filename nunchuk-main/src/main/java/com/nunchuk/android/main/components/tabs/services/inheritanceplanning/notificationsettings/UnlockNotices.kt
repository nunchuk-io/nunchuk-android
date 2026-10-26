package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.main.R
import com.nunchuk.android.model.inheritance.EmailNotificationSettings

/*
 * Unlock notices of an on-chain plan with fallbacks (Figma Row 5 · NOTIF-03, Row 6 · REV).
 *
 * A recipient picks which timelocks they hear about: primary inheritance is the existing
 * `notify_timelock_expires`, each fallback is its zero-based index in `notify_fallbacks`. The
 * "Notify them when a timelock expires" switch is on while any of them is picked.
 */

/** One timelock a recipient can be told about. */
sealed interface UnlockNoticeRoute {
    data object Primary : UnlockNoticeRoute

    /** Zero-based, as `notify_fallbacks` holds it. */
    data class Fallback(val index: Int) : UnlockNoticeRoute
}

/** What this recipient hears about: primary first, then fallbacks in order. Indexes past [fallbackCount] are dropped. */
fun EmailNotificationSettings.unlockNoticeRoutes(fallbackCount: Int): List<UnlockNoticeRoute> = buildList {
    if (notifyOnTimelockExpiry) add(UnlockNoticeRoute.Primary)
    notifyFallbacks.orEmpty()
        .filter { it in 0 until fallbackCount }
        .distinct()
        .sorted()
        .forEach { add(UnlockNoticeRoute.Fallback(it)) }
}

/** The master switch: on while any timelock is picked. */
fun EmailNotificationSettings.notifiesOnAnyUnlock(fallbackCount: Int): Boolean =
    unlockNoticeRoutes(fallbackCount).isNotEmpty()

/** Switch on: every timelock is picked. Switch off: none, and fallback notices are sent as `[]` (off). */
fun EmailNotificationSettings.withAllUnlockNotices(enabled: Boolean, fallbackCount: Int): EmailNotificationSettings =
    copy(
        notifyOnTimelockExpiry = enabled,
        notifyFallbacks = if (enabled) List(fallbackCount) { it } else emptyList(),
    )

fun EmailNotificationSettings.withUnlockNotice(
    route: UnlockNoticeRoute,
    checked: Boolean,
    fallbackCount: Int,
): EmailNotificationSettings = when (route) {
    UnlockNoticeRoute.Primary -> copy(notifyOnTimelockExpiry = checked)
    is UnlockNoticeRoute.Fallback -> {
        val current = notifyFallbacks.orEmpty().filter { it in 0 until fallbackCount }.toSet()
        copy(notifyFallbacks = (if (checked) current + route.index else current - route.index).sorted())
    }
}

@Composable
internal fun UnlockNoticeRoute.label(): String = when (this) {
    UnlockNoticeRoute.Primary -> stringResource(R.string.nc_primary_inheritance_section)
    is UnlockNoticeRoute.Fallback -> stringResource(R.string.nc_fallback_number, index + 1)
}

/** "Primary inheritance", "Fallback 1, Fallback 2", or "None" — the Review row's value. */
@Composable
internal fun EmailNotificationSettings.unlockNoticesSummary(fallbackCount: Int): String {
    val routes = unlockNoticeRoutes(fallbackCount)
    if (routes.isEmpty()) return stringResource(com.nunchuk.android.core.R.string.nc_none)
    return routes.map { it.label() }.joinToString(", ")
}
