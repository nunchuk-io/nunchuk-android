package com.nunchuk.android.auth.components.signin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleSignInCallbackValidatorTest {
    private val state = "android_abc"
    private val startedAt = 1_000_000L

    private fun accepted(
        pendingState: String = state,
        now: Long = startedAt + 30_000L,
        callbackState: String? = state,
    ) = AppleSignInCallbackValidator.isAccepted(pendingState, startedAt, now, callbackState)

    @Test
    fun `matching state within the window is accepted`() = assertTrue(accepted())

    @Test
    fun `callback with no sign-in in flight is rejected`() = assertFalse(accepted(pendingState = ""))

    @Test
    fun `mismatched state is rejected`() = assertFalse(accepted(callbackState = "android_other"))

    @Test
    fun `callback without state is rejected`() = assertFalse(accepted(callbackState = null))

    @Test
    fun `callback after the timeout is rejected`() =
        assertFalse(accepted(now = startedAt + AppleSignInCallbackValidator.PENDING_TIMEOUT_MS + 1))

    @Test
    fun `callback dated before the request is rejected`() = assertFalse(accepted(now = startedAt - 1))
}
