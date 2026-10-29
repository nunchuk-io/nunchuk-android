package com.nunchuk.android.auth.components.signin

import java.util.concurrent.TimeUnit

/**
 * Decides whether a `nunchuk://callback` Apple Sign-In redirect may be applied.
 *
 * The callback activity is exported, so any app or link can deliver one. It is accepted only
 * while this device has an Apple sign-in in flight and its `state` (echoed by the backend)
 * matches the one sent to Apple.
 */
internal object AppleSignInCallbackValidator {
    val PENDING_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10)

    fun isAccepted(
        pendingState: String,
        pendingStartedAt: Long,
        now: Long,
        callbackState: String?,
    ): Boolean {
        if (pendingState.isEmpty()) return false
        if (now - pendingStartedAt !in 0..PENDING_TIMEOUT_MS) return false
        return callbackState == pendingState
    }
}
