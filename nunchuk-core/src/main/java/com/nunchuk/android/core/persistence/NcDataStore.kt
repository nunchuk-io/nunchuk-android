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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.type.Chain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val NAME = "nc_data_store"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = NAME)

@Singleton
class NcDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val accountManager: AccountManager,
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
    private val useLargeFontHomeBalances = booleanPreferencesKey("use_large_font_home_balances")
    private val showNewPortalKey = booleanPreferencesKey("show_new_portal")
    private val passwordTokenKey = stringPreferencesKey("password_token")

    /**
     * Current membership plan key
     */
    private val localMembershipPlanKey = intPreferencesKey("membership_plan")
    private val membershipPlansKey = stringSetPreferencesKey("membership_plans")

    private fun getWalletSecuritySettingKey(): Preferences.Key<String> {
        val userId = accountManager.getAccount().chatId
        return stringPreferencesKey("wallet_security_setting_key-${userId}")
    }

    private fun getLocalCurrencyKey(): Preferences.Key<String> {
        val userId = accountManager.getAccount().chatId
        return stringPreferencesKey("local_currency-${userId}")
    }

    private fun getShowOnBoardKey(): Preferences.Key<Boolean> {
        val userId = accountManager.getAccount().chatId
        return booleanPreferencesKey("show-onboard-${userId}")
    }

    private fun getShowHealthCheckReminderIntroKey(): Preferences.Key<Boolean> {
        val userId = accountManager.getAccount().chatId
        return booleanPreferencesKey("show_health_check_reminder_intro-${userId}")
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

    val isShowHealthCheckReminderIntro: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[getShowHealthCheckReminderIntroKey()] ?: true
        }

    val turnOnNotificationFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[turnOnNotificationKey] ?: true
        }

    val localMembershipPlan: Flow<MembershipPlan>
        get() = context.dataStore.data.map {
            val ordinal = it[localMembershipPlanKey] ?: 0
            MembershipPlan.entries[ordinal]
        }

    val chain: Flow<Chain>
        get() = context.dataStore.data.map {
            Chain.entries[it[chainKey] ?: 0]
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

    suspend fun setLocalMembershipPlan(plan: MembershipPlan) {
        context.dataStore.edit {
            it[localMembershipPlanKey] = plan.ordinal
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

    val plans: Flow<List<MembershipPlan>>
        get() = context.dataStore.data.map {
            it[membershipPlansKey].orEmpty().map { plan ->
                plan.toMembershipPlan()
            }
        }

    suspend fun setPlans(plans: List<String>) {
        context.dataStore.edit {
            it[membershipPlansKey] = plans.toSet()
        }
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

    suspend fun setUseLargeFontHomeBalances(largeFont: Boolean) {
        context.dataStore.edit {
            it[useLargeFontHomeBalances] = largeFont
        }
    }

    val useLargeFontHomeBalancesFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[useLargeFontHomeBalances] ?: false
        }

    val currentStep: Flow<MembershipStep?>
        get() = context.dataStore.data.map {
            it[currentStepKey]?.let { index -> MembershipStep.entries[index] }
        }

    suspend fun setCurrentStep(step: MembershipStep) {
        context.dataStore.edit {
            it[currentStepKey] = step.ordinal
        }
    }

    val showOnBoard: Flow<Boolean?>
        get() = context.dataStore.data.map {
            it[getShowOnBoardKey()]
        }

    suspend fun setShowOnBoard(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[getShowOnBoardKey()] = value
        }
    }

    suspend fun checkShowOnboardForFreshInstall() {
        context.dataStore.edit { settings ->
            if (settings.asMap().size <= 1) {
                settings[getShowOnBoardKey()] = true
                accountManager.setShouldShowOnboard(true)
            }
        }
    }

    suspend fun setHealthCheckReminderIntro(isShow: Boolean) {
        context.dataStore.edit {
            it[getShowHealthCheckReminderIntroKey()] = isShow
        }
    }

    suspend fun setPasswordToken(token: String) {
        context.dataStore.edit {
            it[passwordTokenKey] = token
        }
    }

    val passwordToken: Flow<String>
        get() = context.dataStore.data.map {
            it[passwordTokenKey].orEmpty()
        }

    suspend fun shouldShowNewPortal() : Boolean {
        return context.dataStore.data.map {
            it[showNewPortalKey] ?: true
        }.first()
    }

    suspend fun setShowPortal(show: Boolean) {
        context.dataStore.edit {
            it[showNewPortalKey] = show
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(syncEnableKey)
            it.remove(turnOnNotificationKey)
            it.remove(localMembershipPlanKey)
            it.remove(hideUpsellBannerKey)
            it.remove(syncRoomSuccessKey)
            it.remove(assistedKeysPreferenceKey)
            it.remove(securityQuestionKey)
            it.remove(groupIdKey)
            it.remove(groupAssistedKeysPreferenceKey)
            it.remove(membershipPlansKey)
        }
    }
}