package com.nunchuk.android.nav

import android.content.Context

interface MainNavigator {
    fun openMainScreen(
        activityContext: Context,
        loginHalfToken: String? = null,
        deviceId: String? = null,
        bottomNavViewPosition: Int? = null,
        isNewDevice: Boolean = false
    )

    fun openGuestModeIntroScreen(activityContext: Context)
    fun openGuestModeMessageIntroScreen(activityContext: Context)
}