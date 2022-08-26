package com.nunchuk.android.signer.software.components.primarykey.manuallyusername

sealed class PKeyManuallyUsernameEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyManuallyUsernameEvent()
    object ProcessFailure : PKeyManuallyUsernameEvent()
    data class CheckUsernameSuccess(val username: String) : PKeyManuallyUsernameEvent()
}

data class PKeyManuallyUsernameState(
    val username: String = ""
)