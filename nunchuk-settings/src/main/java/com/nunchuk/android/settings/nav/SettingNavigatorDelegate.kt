package com.nunchuk.android.settings.nav

import android.content.Context
import com.nunchuk.android.nav.SettingNavigator
import com.nunchuk.android.settings.developer.DeveloperSettingActivity
import com.nunchuk.android.settings.devices.UserDevicesActivity
import com.nunchuk.android.settings.network.NetworkSettingActivity
import com.nunchuk.android.settings.sync.SyncSettingActivity
import com.nunchuk.android.settings.unit.DisplayUnitActivity

interface SettingNavigatorDelegate : SettingNavigator {

    override fun openNetworkSettingScreen(activityContext: Context) {
        NetworkSettingActivity.start(activityContext)
    }

    override fun openDisplayUnitScreen(activityContext: Context) {
        DisplayUnitActivity.start(activityContext)
    }

    override fun openDeveloperScreen(activityContext: Context) {
        DeveloperSettingActivity.start(activityContext)
    }

    override fun openSyncSettingScreen(activityContext: Context) {
        SyncSettingActivity.start(activityContext)
    }

    override fun openUserDevicesScreen(activityContext: Context) {
        UserDevicesActivity.start(activityContext)
    }
}