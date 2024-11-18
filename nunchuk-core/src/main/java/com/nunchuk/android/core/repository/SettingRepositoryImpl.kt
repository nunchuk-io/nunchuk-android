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

package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.persistence.NcEncryptedPreferences
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class SettingRepositoryImpl @Inject constructor(
    private val ncDataStore: NcDataStore,
    private val ncEncryptedPreferences: NcEncryptedPreferences,
    private val gson: Gson,
    private val accountManager: AccountManager
) : SettingRepository {
    override val syncEnable: Flow<Boolean>
        get() = ncDataStore.syncEnableFlow

    override val isShowNfcUniversal: Flow<Boolean>
        get() = ncDataStore.isShowNfcUniversal

    override val chain: Flow<Chain>
        get() = ncDataStore.chain

    override val syncRoomSuccess: Flow<Boolean>
        get() = ncDataStore.syncRoomSuccess

    override val qrDensity: Flow<Int>
        get() = ncDataStore.qrDensity

    override val walletSecuritySetting: Flow<WalletSecuritySetting>
        get() = ncDataStore.walletSecuritySetting

    override val homeDisplaySetting: Flow<HomeDisplaySetting>
        get() = ncDataStore.homeDisplaySetting

    override val walletPin: Flow<String>
        get() = ncEncryptedPreferences.getWalletPinFlow()

    override val localCurrency: Flow<String>
        get() = ncDataStore.localCurrencyFlow

    override val useLargeFontHomeBalances: Flow<Boolean>
        get() = ncDataStore.useLargeFontHomeBalancesFlow

    override val isShowHealthCheckReminderIntro: Flow<Boolean>
        get() = ncDataStore.isShowHealthCheckReminderIntro

    override val referrerCode: Flow<ReferrerCode?>
        get() = ncDataStore.referralCodeFlow.map {
            runCatching {
                gson.fromJson(it, ReferrerCode::class.java)
            }.getOrNull()
        }
    override val campaign: Flow<Campaign?>
        get() = ncDataStore.getCampaignFlow(accountManager.getAccount().email).map {
            runCatching {
                gson.fromJson(it, Campaign::class.java)
            }.getOrNull()
        }

    override suspend fun markSyncRoomSuccess() {
        ncDataStore.markSyncRoomSuccess()
    }

    override suspend fun setSyncEnable(isEnable: Boolean) {
        ncDataStore.setSyncEnable(isEnable)
    }

    override suspend fun setQrDensity(density: Int) {
        ncDataStore.setDensity(density)
    }

    override suspend fun markIsShowNfcUniversal() {
        ncDataStore.markIsShowNfcUniversal()
    }

    override suspend fun setWalletSecuritySetting(config: String) {
        ncDataStore.setWalletSecuritySetting(config)
    }

    override suspend fun setHomeDisplaySetting(config: String) {
        ncDataStore.setHomeDisplaySetting(config)
    }

    override suspend fun setWalletPin(pin: String) {
        ncEncryptedPreferences.setWalletPin(pin)
    }

    override suspend fun setLocalCurrency(currency: String) {
        ncDataStore.setLocalCurrency(currency)
    }

    override suspend fun setLocalMembershipPlan(plan: MembershipPlan) {
        ncDataStore.setLocalMembershipPlan(plan)
    }

    override suspend fun setHealthCheckReminderIntro(isShow: Boolean) {
        ncDataStore.setHealthCheckReminderIntro(isShow)
    }

    override suspend fun setReferrerCode(code: String) {
        ncDataStore.setReferrerCode(code)
    }

    override suspend fun setCampaign(campaign: String, email: String) {
        ncDataStore.setCampaign(campaign, email)
    }

    override suspend fun setLastCloseApp(time: Long) {
        ncDataStore.setLastCloseApp(time)
    }

    override val lastCloseApp: Flow<Long>
        get() = ncDataStore.lastCloseApp

    override val displayTotalBalance: Flow<Boolean>
        get() = ncDataStore.displayTotalBalanceFlow

    override fun getCustomPinConfig(decoyPin: String): Flow<Boolean> = ncDataStore.getCustomPinConfig(decoyPin)

    override suspend fun setCustomPinConfig(decoyPin: String, isEnable: Boolean) {
        ncDataStore.setCustomPinConfig(decoyPin, isEnable)
    }

    override suspend fun setDarkMode(isDarkMode: Boolean?) {
        ncDataStore.setDarkMode(isDarkMode)
    }

    override fun getDarkMode(): Flow<Boolean?> = ncDataStore.isDarkMode
}