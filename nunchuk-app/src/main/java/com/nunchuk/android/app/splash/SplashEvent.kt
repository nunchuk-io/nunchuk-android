package com.nunchuk.android.app.splash

sealed class SplashEvent {

    object NavCreateAccountEvent : SplashEvent()

    object NavActivateAccountEvent : SplashEvent()

    object NavSignInEvent : SplashEvent()

    object NavHomeScreenEvent : SplashEvent()

    data class InitErrorEvent(val error: String) : SplashEvent()

}