package com.nunchuk.android.nav

import android.content.Context

interface SettingNavigator {
    fun openNetworkSettingScreen(activityContext: Context)
    fun openDisplayUnitScreen(activityContext: Context)
    fun openDeveloperScreen(activityContext: Context)
    fun openSyncSettingScreen(activityContext: Context)
    fun openUserDevicesScreen(activityContext: Context)
}