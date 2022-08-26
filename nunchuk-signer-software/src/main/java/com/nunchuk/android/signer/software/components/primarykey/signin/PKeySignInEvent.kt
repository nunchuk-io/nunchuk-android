package com.nunchuk.android.signer.software.components.primarykey.signin

import com.nunchuk.android.model.PrimaryKey

sealed class PKeySignInEvent {
    data class LoadingEvent(val loading: Boolean) : PKeySignInEvent()
    data class ProcessErrorEvent(val message: String) : PKeySignInEvent()
    object SignInSuccessEvent : PKeySignInEvent()
    data class InitFailure(val message: String) : PKeySignInEvent()
}

data class PKeySignInState(
    val primaryKey: PrimaryKey? = null,
    val staySignedIn: Boolean = false
)