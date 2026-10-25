package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.ui.toTimeZoneDetail
import com.nunchuk.android.main.R
import com.nunchuk.android.model.TimelockExtra
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** "Set up Fallback N timelock" (TIME-01) or, once it has a time, "Edit Fallback N timelock" (TIME-02). */
@Composable
internal fun fallbackTimelockTitle(target: FallbackTimelockTarget, isEdit: Boolean): String =
    stringResource(if (isEdit) R.string.nc_edit_fallback_timelock else R.string.nc_set_up_fallback_timelock, target.number)

/**
 * The body of the fallback timelock screen. TIME-01: what the time means, that it is final, and the
 * unlock time before it ("Not set yet" while unset). TIME-02: the ordering rule and both limits;
 * "Must be before …" only when a later fallback has a time.
 */
@Composable
internal fun FallbackTimelockIntro(
    target: FallbackTimelockTarget,
    isEdit: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isEdit) {
            Text(text = stringResource(R.string.nc_edit_fallback_timelock_desc), style = NunchukTheme.typography.body)
            NeighbourBox {
                NeighbourRow(
                    label = if (target.index == 0) {
                        stringResource(R.string.nc_must_be_after_primary_inheritance)
                    } else {
                        stringResource(R.string.nc_must_be_after_fallback, target.index)
                    },
                    timelock = target.previous,
                )
                target.next?.let {
                    NeighbourRow(label = stringResource(R.string.nc_must_be_before_fallback, target.number + 1), timelock = it)
                }
            }
        } else {
            Text(
                text = if (target.index == 0) {
                    stringResource(R.string.nc_set_up_fallback_timelock_desc_primary, target.number)
                } else {
                    stringResource(R.string.nc_set_up_fallback_timelock_desc_fallback, target.number, target.index)
                },
                style = NunchukTheme.typography.body,
            )
            Text(text = stringResource(R.string.nc_set_up_fallback_timelock_immutable), style = NunchukTheme.typography.body)
            NeighbourBox {
                NeighbourRow(
                    label = if (target.index == 0) {
                        stringResource(R.string.nc_primary_inheritance_unlocks_after)
                    } else {
                        stringResource(R.string.nc_fallback_unlocks_after, target.index)
                    },
                    timelock = target.previous,
                )
            }
        }
    }
}

@Composable
private fun NeighbourBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.nc_grey_light), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun NeighbourRow(label: String, timelock: TimelockExtra?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = NunchukTheme.typography.bodySmall, color = MaterialTheme.colorScheme.textSecondary)
        Text(
            text = timelock?.takeIf { it.value > 0 }?.let(::formatTimelockWithZone)
                ?: stringResource(R.string.nc_timelock_not_set_yet),
            style = NunchukTheme.typography.title,
        )
    }
}

/** "Even with the fallback key(s), no one can claim through Fallback N before this time." */
@Composable
internal fun fallbackTimelockHint(target: FallbackTimelockTarget): String =
    pluralStringResource(R.plurals.nc_fallback_timelock_hint, target.keyCount, target.number)

/** The Invalid date message for a time outside the fallback's limits (TIME-03 / TIME-04). */
@Composable
internal fun fallbackBoundMessage(target: FallbackTimelockTarget, bound: FallbackTimelockBound): String = when (bound) {
    FallbackTimelockBound.TOO_EARLY -> {
        val previous = target.previous?.let(::formatTimelockWithZone).orEmpty()
        if (target.index == 0) {
            stringResource(R.string.nc_fallback_timelock_too_early_primary, target.number, previous)
        } else {
            stringResource(R.string.nc_fallback_timelock_too_early_fallback, target.number, target.index, previous)
        }
    }

    FallbackTimelockBound.TOO_LATE -> stringResource(
        R.string.nc_fallback_timelock_too_late,
        target.number,
        target.number + 1,
        target.next?.let(::formatTimelockWithZone).orEmpty(),
    )
}

/** "05/29/2040 01:25 (GMT+07:00)" in the lock's own time zone. */
internal fun formatTimelockWithZone(timelock: TimelockExtra): String {
    val zoneId = timelock.timezone?.takeIf { it.isNotEmpty() }
    val format = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
    zoneId?.let { format.timeZone = TimeZone.getTimeZone(it) }
    val date = format.format(Date(timelock.value * 1000))
    val offset = zoneId?.let { runCatching { it.toTimeZoneDetail()?.offset }.getOrNull() }
    return if (offset.isNullOrEmpty()) date else "$date ($offset)"
}

private val previewTarget = FallbackTimelockTarget(
    index = 1,
    keyCount = 2,
    previous = TimelockExtra(value = 2221988700L, timezone = "Asia/Jakarta"),
    next = TimelockExtra(value = 2537521500L, timezone = "Asia/Jakarta"),
)

@PreviewLightDark
@Composable
private fun FallbackTimelockIntroPreview() {
    NunchukTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FallbackTimelockIntro(target = previewTarget, isEdit = true)
            FallbackTimelockIntro(target = previewTarget.copy(index = 0, previous = null, next = null), isEdit = false)
        }
    }
}
