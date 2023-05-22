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

package com.nunchuk.android.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.type.Chain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val NAME = "nc_data_store"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = NAME)

@Singleton
class NcDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val accountManager: AccountManager
) {
    private val btcPriceKey = doublePreferencesKey("btc_price")
    private val turnOnNotificationKey = booleanPreferencesKey("turn_on_notification")
    private val syncEnableKey = booleanPreferencesKey("sync_enable")
    private val isShowNfcUniversalKey = booleanPreferencesKey("show_nfc_universal")
    private val chainKey = intPreferencesKey("chain")
    private val hideUpsellBannerKey = booleanPreferencesKey("hide_upsell_banner")
    private val syncRoomSuccessKey = booleanPreferencesKey("sync_room_success")
    private val qrDensityKey = intPreferencesKey("qr_density")
    private val assistedKeysPreferenceKey = stringSetPreferencesKey("assisted_key")
    private val groupAssistedKeysPreferenceKey = stringSetPreferencesKey("group_assisted_key")
    private val feeJsonPreferenceKey = stringPreferencesKey("fee_key")
    private val securityQuestionKey = booleanPreferencesKey("security_question")
    private val groupIdKey = stringPreferencesKey("group_id")
    private val currentStepKey = intPreferencesKey("current_step")

    /**
     * Current membership plan key
     */
    private val membershipPlanKey = intPreferencesKey("membership_plan")

    private fun getWalletSecuritySettingKey(): Preferences.Key<String> {
        val userId = accountManager.getAccount().chatId
        return stringPreferencesKey("wallet_security_setting_key-${userId}")
    }

    private fun getLocalCurrencyKey(): Preferences.Key<String> {
        val userId = accountManager.getAccount().chatId
        return stringPreferencesKey("local_currency-${userId}")
    }

    val syncRoomSuccess: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[syncRoomSuccessKey] ?: false
        }

    val groupId: Flow<String>
        get() = context.dataStore.data.map {
            it[groupIdKey].orEmpty()
        }

    suspend fun markSyncRoomSuccess() {
        context.dataStore.edit { settings ->
            settings[syncRoomSuccessKey] = true
        }
    }

    val btcPriceFlow: Flow<Double>
        get() = context.dataStore.data.map { it[btcPriceKey] ?: 45000.0 }

    val syncEnableFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[syncEnableKey] ?: false
        }

    val isShowNfcUniversal: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[isShowNfcUniversalKey] ?: true
        }

    val turnOnNotificationFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[turnOnNotificationKey] ?: true
        }

    val membershipPlan: Flow<MembershipPlan>
        get() = context.dataStore.data.map {
            val ordinal = it[membershipPlanKey] ?: 0
            MembershipPlan.values()[ordinal]
        }

    val chain: Flow<Chain>
        get() = context.dataStore.data.map {
            Chain.values()[it[chainKey] ?: 0]
        }

    val isHideUpsellBanner: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[hideUpsellBannerKey] ?: false
        }

    val qrDensity: Flow<Int>
        get() = context.dataStore.data.map {
            it[qrDensityKey] ?: 200
        }

    val walletSecuritySetting: Flow<WalletSecuritySetting>
        get() = context.dataStore.data.map {
            gson.fromJson(
                it[getWalletSecuritySettingKey()].orEmpty(),
                WalletSecuritySetting::class.java
            )
        }

    val localCurrencyFlow: Flow<String>
        get() = context.dataStore.data.map {
            it[getLocalCurrencyKey()] ?: USD_CURRENCY
        }

    suspend fun setChain(chain: Chain) {
        context.dataStore.edit { settings ->
            settings[chainKey] = chain.ordinal
        }
    }

    suspend fun updateTurnOnNotification(turnOn: Boolean) {
        context.dataStore.edit { settings ->
            settings[turnOnNotificationKey] = turnOn
        }
    }

    suspend fun updateBtcPrice(price: Double) {
        context.dataStore.edit { settings ->
            settings[btcPriceKey] = price
        }
    }

    suspend fun setSyncEnable(isEnable: Boolean) {
        context.dataStore.edit { settings ->
            settings[syncEnableKey] = isEnable
        }
    }

    suspend fun markIsShowNfcUniversal() {
        context.dataStore.edit { settings ->
            settings[isShowNfcUniversalKey] = false
        }
    }

    suspend fun setMembershipPlan(plan: MembershipPlan) {
        context.dataStore.edit {
            it[membershipPlanKey] = plan.ordinal
        }
    }

    suspend fun setHideUpsellBanner() {
        context.dataStore.edit {
            it[hideUpsellBannerKey] = true
        }
    }

    suspend fun setDensity(density: Int) {
        context.dataStore.edit {
            it[qrDensityKey] = density
        }
    }

    suspend fun setWalletSecuritySetting(config: String) {
        context.dataStore.edit {
            it[getWalletSecuritySettingKey()] = config
        }
    }

    suspend fun setLocalCurrency(currency: String) {
        context.dataStore.edit {
            it[getLocalCurrencyKey()] = currency
        }
    }

    suspend fun setAssistedKey(keys: Set<String>) {
        context.dataStore.edit {
            it[assistedKeysPreferenceKey] = keys
        }
    }

    val assistedKeys: Flow<Set<String>>
        get() = context.dataStore.data.map {
            it[assistedKeysPreferenceKey] ?: emptySet()
        }

    suspend fun setGroupAssistedKey(keys: Set<String>) {
        context.dataStore.edit {
            it[groupAssistedKeysPreferenceKey] = keys
        }
    }

    val groupAssistedKeys: Flow<Set<String>>
        get() = context.dataStore.data.map {
            it[groupAssistedKeysPreferenceKey] ?: emptySet()
        }

    suspend fun setFeeJsonString(feeJson: String) {
        context.dataStore.edit {
            it[feeJsonPreferenceKey] = feeJson
        }
    }

    val fee: Flow<String>
        get() = context.dataStore.data.map {
            it[feeJsonPreferenceKey].orEmpty()
        }

    suspend fun setSetupSecurityQuestion(isSetup: Boolean) {
        context.dataStore.edit {
            it[securityQuestionKey] = isSetup
        }
    }

    val isSetupSecurityQuestion: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[securityQuestionKey] ?: false
        }

    val currentStep: Flow<MembershipStep?>
        get() = context.dataStore.data.map {
            it[currentStepKey]?.let { index -> MembershipStep.values()[index] }
        }

    suspend fun setCurrentStep(step: MembershipStep) {
        context.dataStore.edit {
            it[currentStepKey] = step.ordinal
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(syncEnableKey)
            it.remove(turnOnNotificationKey)
            it.remove(membershipPlanKey)
            it.remove(hideUpsellBannerKey)
            it.remove(syncRoomSuccessKey)
            it.remove(assistedKeysPreferenceKey)
            it.remove(securityQuestionKey)
            it.remove(groupIdKey)
        }
    }
}