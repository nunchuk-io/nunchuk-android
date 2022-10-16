package com.nunchuk.android.signer.software.components.primarykey.chooseusername

sealed class PKeyChooseUsernameEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyChooseUsernameEvent()
    data class ProcessFailure(val message: String) : PKeyChooseUsernameEvent()
    data class GetDefaultUsernameSuccess(val username: String) : PKeyChooseUsernameEvent()
    object SignUpSuccess : PKeyChooseUsernameEvent()
    data class GetTurnOnNotificationSuccess(val isTurnOn: Boolean) : PKeyChooseUsernameEvent()
}

data class PKeyChooseUsernameEventState(
    val username: String = "",
    val confirmPassphrase: String = "",
    val defaultUserName: String? = null,
)