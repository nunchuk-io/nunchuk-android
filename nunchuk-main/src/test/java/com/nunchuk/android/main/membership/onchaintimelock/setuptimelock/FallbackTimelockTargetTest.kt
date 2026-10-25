package com.nunchuk.android.main.membership.onchaintimelock.setuptimelock

import com.nunchuk.android.model.TimelockExtra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
