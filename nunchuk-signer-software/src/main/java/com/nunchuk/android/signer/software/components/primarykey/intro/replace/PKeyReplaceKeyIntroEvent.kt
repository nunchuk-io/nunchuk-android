package com.nunchuk.android.signer.software.components.primarykey.intro.replace

sealed class PKeyReplaceKeyIntroEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyReplaceKeyIntroEvent()
    data class CheckNeedPassphraseSent(val isNeeded: Boolean) : PKeyReplaceKeyIntroEvent()
}