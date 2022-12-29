package com.nunchuk.android.main.components.tabs.services.keyrecovery.keyrecoverysuccess

sealed class KeyRecoverySuccessEvent {
    object GotItClick : KeyRecoverySuccessEvent()
}