/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.settings.nav

import android.content.Context
import android.content.Intent
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.nav.SettingNavigator
import com.nunchuk.android.settings.about.AboutActivity
import com.nunchuk.android.settings.developer.DeveloperSettingActivity
import com.nunchuk.android.settings.devices.UserDevicesActivity
import com.nunchuk.android.settings.displaysettings.DisplaySettingsActivity
import com.nunchuk.android.settings.localcurrency.LocalCurrencyActivity
import com.nunchuk.android.settings.network.NetworkSettingActivity
import com.nunchuk.android.settings.notification.TurnNotificationActivity
import com.nunchuk.android.settings.sync.SyncSettingActivity
import com.nunchuk.android.settings.unit.DisplayUnitActivity
import com.nunchuk.android.settings.walletsecurity.WalletSecuritySettingActivity
import com.nunchuk.android.settings.walletsecurity.unlock.UnlockPinActivity
import com.nunchuk.android.settings.walletvisibilitysettings.WalletVisibilitySettingsActivity

interface SettingNavigatorDelegate : SettingNavigator {

    override fun openNetworkSettingScreen(activityContext: Context) {
        NetworkSettingActivity.start(activityContext)
    }

    override fun openDisplayUnitScreen(activityContext: Context) {
        DisplayUnitActivity.start(activityContext)
    }

    override fun openDisplaySettingsScreen(activityContext: Context) {
        DisplaySettingsActivity.start(activityContext)
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

    override fun openAboutScreen(activityContext: Context) {
        AboutActivity.start(activityContext)
    }

    override fun openWalletSecuritySettingScreen(activityContext: Context, args: WalletSecurityArgs) {
        WalletSecuritySettingActivity.start(activityContext, args)
    }

    override fun openWalletVisibilitySettingsScreen(activityContext: Context) {
        WalletVisibilitySettingsActivity.start(activityContext)
    }

    override fun openLocalCurrencyScreen(activityContext: Context) {
        LocalCurrencyActivity.start(activityContext)
    }

    override fun openUnlockPinScreen(activityContext: Context) {
        activityContext.startActivity(
            Intent(
                activityContext,
                UnlockPinActivity::class.java
            )
        )
    }

    override fun openTurnNotificationScreen(
        activityContext: Context,
        messages: ArrayList<String>
    ) {
        TurnNotificationActivity.start(
            activityContext,
            messages = messages
        )
    }
}