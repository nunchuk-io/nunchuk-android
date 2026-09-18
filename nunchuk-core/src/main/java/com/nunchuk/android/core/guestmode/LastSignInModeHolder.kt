package com.nunchuk.android.core.guestmode

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastSignInModeHolder @Inject constructor() {
    private var lastLoginDecoyPin = ""

    // Never log lastLoginDecoyPin: it is the user's decoy PIN in cleartext.
    fun getLastLoginDecoyPin(): String {
        return lastLoginDecoyPin
    }

    fun setLastLoginDecoyPin(pin: String) {
        lastLoginDecoyPin = pin
    }

    fun clear() {
        lastLoginDecoyPin = ""
    }
}
