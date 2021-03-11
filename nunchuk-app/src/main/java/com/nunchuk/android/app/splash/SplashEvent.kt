package com.nunchuk.android.app.splash

sealed class SplashEvent {

    object InitNunchukCompleted : SplashEvent()

    data class InitNunchukError(val error: String?) : SplashEvent()

}