package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import android.os.Parcelable
import com.nunchuk.android.model.TimelockExtra
import kotlinx.parcelize.Parcelize

/**
 * The fallback whose unlock time the timelock screen sets (TIME-01 / TIME-02), with its neighbours.
 * Unlock times are strictly increasing: primary < Fallback 1 < Fallback 2 < …
 *
 * @property index zero-based fallback index; shown as `index + 1`.
 * @property keyCount the fallback's N, for "the fallback key(s)".
 * @property previous the unlock time it must come after: primary inheritance for Fallback 1, else
 *   the fallback before it; null while that time is not set.
 * @property next the unlock time of the fallback after it, if there is one and it is set.
 */
@Parcelize
data class FallbackTimelockTarget(
    val index: Int,
    val keyCount: Int,
    val previous: TimelockExtra?,
    val next: TimelockExtra?,
) : Parcelable {
    val number: Int get() = index + 1

    /** The bound a [value] (unix seconds) breaks, if any. Equal times are not allowed either. */
    fun boundViolation(value: Long): FallbackTimelockBound? = when {
        previous != null && previous.value > 0 && value <= previous.value -> FallbackTimelockBound.TOO_EARLY
        next != null && next.value > 0 && value >= next.value -> FallbackTimelockBound.TOO_LATE
        else -> null
    }
}

enum class FallbackTimelockBound { TOO_EARLY, TOO_LATE }
