package com.nunchuk.android.app.splash

sealed class SplashEvent {

    object NavActivateAccountEvent : SplashEvent()

    object NavSignInEvent : SplashEvent()

    object NavIntroEvent : SplashEvent()

    object NavHomeScreenEvent : SplashEvent()

    data class InitErrorEvent(val error: String) : SplashEvent()

}