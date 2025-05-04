package com.nunchuk.android.core.guestmode

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastSignInModeHolder @Inject constructor() {
    private var lastLoginDecoyPin = ""

    fun getLastLoginDecoyPin(): String {
        Timber.tag("LastSignInModeHolder").e("getLastLoginDecoyPin: $lastLoginDecoyPin")
        return lastLoginDecoyPin
    }

    fun setLastLoginDecoyPin(pin: String) {
        Timber.tag("LastSignInModeHolder").e("setLastLoginDecoyPin: $pin")
        lastLoginDecoyPin = pin
    }

    fun clear() {
        lastLoginDecoyPin = ""
    }
}