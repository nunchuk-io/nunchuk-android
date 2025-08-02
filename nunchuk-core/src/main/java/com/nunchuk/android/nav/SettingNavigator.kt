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

package com.nunchuk.android.nav

import android.content.Context
import com.nunchuk.android.core.util.UnlockPinSourceFlow
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.nav.args.FeeSettingArgs

interface SettingNavigator {
    fun openNetworkSettingScreen(activityContext: Context)
    fun openDisplayUnitScreen(activityContext: Context)
    fun openDisplaySettingsScreen(activityContext: Context)
    fun openDeveloperScreen(activityContext: Context)
    fun openSyncSettingScreen(activityContext: Context)
    fun openUserDevicesScreen(activityContext: Context)
    fun openAboutScreen(activityContext: Context)
    fun openWalletSecuritySettingScreen(activityContext: Context, args: WalletSecurityArgs)
    fun openTurnNotificationScreen(
        activityContext: Context,
        messages: ArrayList<String> = arrayListOf()
    )
    fun openLocalCurrencyScreen(activityContext: Context)
    fun openUnlockPinScreen(activityContext: Context, sourceFlowInfo: Int = UnlockPinSourceFlow.NONE)
    fun openBiometricScreen(activityContext: Context)
    fun openFeeSettingsScreen(activityContext: Context, args: FeeSettingArgs)
}