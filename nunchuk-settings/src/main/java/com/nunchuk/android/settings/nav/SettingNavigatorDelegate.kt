package com.nunchuk.android.settings.nav

import android.content.Context
import com.nunchuk.android.nav.SettingNavigator
import com.nunchuk.android.settings.network.NetworkSettingActivity

interface SettingNavigatorDelegate : SettingNavigator {

    override fun openNetworkSettingScreen(activityContext: Context) {
        NetworkSettingActivity.start(activityContext)
    }

}