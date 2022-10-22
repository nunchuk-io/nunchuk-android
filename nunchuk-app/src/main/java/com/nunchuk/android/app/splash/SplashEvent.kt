package com.nunchuk.android.app.splash

sealed class SplashEvent {

    object NavActivateAccountEvent : SplashEvent()

    object NavSignInEvent : SplashEvent()

    data class NavHomeScreenEvent(
        val loginHalfToken: String?,
        val deviceId: String?
    ) : SplashEvent()

    data class InitErrorEvent(val error: String) : SplashEvent()

}