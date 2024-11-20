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

package com.nunchuk.android.repository

import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

interface SettingRepository {
    val syncEnable: Flow<Boolean>
    val isShowNfcUniversal: Flow<Boolean>
    val chain: Flow<Chain>
    val syncRoomSuccess: Flow<Boolean>
    val qrDensity: Flow<Int>
    val walletSecuritySetting: Flow<WalletSecuritySetting>
    val homeDisplaySetting: Flow<HomeDisplaySetting>
    val biometricConfig: Flow<BiometricConfig>
    val walletPin: Flow<String>
    val localCurrency: Flow<String>
    val useLargeFontHomeBalances: Flow<Boolean>
    val isShowHealthCheckReminderIntro: Flow<Boolean>
    val referrerCode: Flow<ReferrerCode?>
    val campaign: Flow<Campaign?>
    val lastCloseApp: Flow<Long>
    val displayTotalBalance: Flow<Boolean>
    suspend fun setSyncEnable(isEnable: Boolean)
    suspend fun setQrDensity(density: Int)
    suspend fun markSyncRoomSuccess()
    suspend fun markIsShowNfcUniversal()
    suspend fun setWalletSecuritySetting(config: String)
    suspend fun setHomeDisplaySetting(config: String)
    suspend fun setBiometricConfig(config: String)
    suspend fun setWalletPin(pin: String)
    suspend fun setLocalCurrency(currency: String)
    suspend fun setLocalMembershipPlan(plan: MembershipPlan)
    suspend fun setHealthCheckReminderIntro(isShow: Boolean)
    suspend fun setReferrerCode(code: String)
    suspend fun setCampaign(campaign: String, email: String)
    suspend fun setLastCloseApp(time: Long)
    suspend fun setCustomPinConfig(decoyPin: String, isEnable: Boolean)
    fun getCustomPinConfig(decoyPin: String): Flow<Boolean>
    suspend fun setDarkMode(isDarkMode: Boolean?)
    fun getDarkMode(): Flow<Boolean?>
}