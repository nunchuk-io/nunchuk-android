package com.nunchuk.android.core.guestmode

object SignInModeHolder {
    var currentMode: SignInMode = SignInMode.NORMAL
}

enum class SignInMode {
    NORMAL, GUEST_MODE;
}

fun SignInMode.isGuestMode() = this == SignInMode.GUEST_MODE