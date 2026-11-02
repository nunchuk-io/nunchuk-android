package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.TimelockExtra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackTimelockTargetTest {

    private val target = FallbackTimelockTarget(
        index = 1,
        keyCount = 2,
        previous = TimelockExtra(value = 2_000),
        next = TimelockExtra(value = 3_000),
    )

    @Test
    fun `a time between the neighbours is allowed`() {
        assertNull(target.boundViolation(2_500))
    }

    @Test
    fun `a time at or before the previous route is too early`() {
        assertEquals(FallbackTimelockBound.TOO_EARLY, target.boundViolation(2_000))
        assertEquals(FallbackTimelockBound.TOO_EARLY, target.boundViolation(1_000))
    }

    @Test
    fun `a time at or after the next fallback is too late`() {
        assertEquals(FallbackTimelockBound.TOO_LATE, target.boundViolation(3_000))
        assertEquals(FallbackTimelockBound.TOO_LATE, target.boundViolation(4_000))
    }

    @Test
    fun `an unset neighbour sets no limit`() {
        val open = target.copy(previous = null, next = TimelockExtra(value = 0))

        assertNull(open.boundViolation(1))
        assertNull(open.boundViolation(10_000))
    }

    @Test
    fun `block height neighbours are compared by the time the backend returns with them`() {
        val heights = target.copy(
            previous = TimelockExtra(value = 2_000, based = TimelockBased.HEIGHT_LOCK, blockHeight = 1_000_004),
            next = TimelockExtra(value = 3_000, based = TimelockBased.HEIGHT_LOCK, blockHeight = 1_000_010),
        )

        assertNull(heights.boundViolation(2_500))
        assertEquals(FallbackTimelockBound.TOO_EARLY, heights.boundViolation(1_500))
        assertEquals(FallbackTimelockBound.TOO_LATE, heights.boundViolation(3_500))
    }

    @Test
    fun `the primary time must be before Fallback 1`() {
        val fallback1 = TimelockExtra(value = 3_000)

        assertFalse(isPrimaryAtOrAfterFirstFallback(2_999, fallback1))
        assertTrue(isPrimaryAtOrAfterFirstFallback(3_000, fallback1))
        assertTrue(isPrimaryAtOrAfterFirstFallback(4_000, fallback1))
        // No fallback, or Fallback 1 has no time yet: no limit.
        assertFalse(isPrimaryAtOrAfterFirstFallback(4_000, null))
        assertFalse(isPrimaryAtOrAfterFirstFallback(4_000, TimelockExtra(value = 0)))
    }
}
