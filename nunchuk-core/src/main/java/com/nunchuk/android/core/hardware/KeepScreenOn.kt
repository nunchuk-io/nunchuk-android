package com.nunchuk.android.core.hardware

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Holds the screen on for as long as the caller is composed.
 *
 * Used by the in-app hardware-signing sheets (Ledger, BitBox). A device conversation is minutes
 * of the user reading a pairing code and approving on the device itself, with nothing to touch on
 * the phone, so the display times out mid-flow — and the phone sleeping there is worse than
 * cosmetic. It can disturb the BLE link, and it drops the sign result: everything downstream of a
 * signed PSBT reports through one-shot events on a `MutableSharedFlow` with no replay, whose
 * collectors are gated at STARTED, so an outcome that lands on a stopped screen is discarded and
 * the key still reads as unsigned when the user comes back.
 *
 * That makes this the mitigation for the dropped result, not a convenience. It does not fix the
 * underlying event bus — an outcome landing while the app is backgrounded is still lost — but it
 * removes the way that actually happens, which is the screen timing out under a flow the user
 * has no reason to touch.
 *
 * Scoped to the sheet rather than the host screen, so the flag is set only while a device is
 * actually being talked to and is cleared however the sheet goes away.
 */
@Composable
fun KeepScreenOn() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
