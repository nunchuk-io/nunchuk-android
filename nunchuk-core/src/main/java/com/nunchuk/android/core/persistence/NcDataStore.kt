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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.model.FeeRate
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.WalletBannerState
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.TaprootFeeSelectionSetting
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
    private val displayTotalBalance = booleanPreferencesKey("display_total_balance")
    private val antiFeeSniping = booleanPreferencesKey("anti_fee_sniping")
    private val showNewPortalKey = booleanPreferencesKey("show_new_portal")
    private val passwordTokenKey = stringPreferencesKey("password_token")
    private val lastCloseAppKey = longPreferencesKey("last_close_app")
    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")
    private val homeDisplaySettingKey = stringPreferencesKey("home_display_setting")
    private val biometricConfigKey = stringPreferencesKey("biometric_config")
    private val groupWalletBackupBannerKeysPreferenceKey = stringSetPreferencesKey("group_wallet_backup_banner_key")
    private val defaultFeeKey = intPreferencesKey("default_fee")
    private val taprootFeeSelectionKey = stringPreferencesKey("taproot_fee_selection")
    private val firstChatIdKey = stringPreferencesKey("first_create_email")
    private val hasWalletInGuestModeKey = booleanPreferencesKey("has_wallet_in_guest_mode")

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

    private fun getCampaignKey(email: String): Preferences.Key<String> {
        return stringPreferencesKey("campaign-${email.hashCode()}")
    }

    private fun getReferrerCodeKey(): Preferences.Key<String> {
        val userId = accountManager.getAccount().chatId
        return stringPreferencesKey("referrer_code-${userId}")
    }

    val syncRoomSuccess: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[syncRoomSuccessKey] == true
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

    suspend fun resetSyncRoomSuccess() {
        context.dataStore.edit { settings ->
            settings[syncRoomSuccessKey] = false
        }
    }

    val btcPriceFlow: Flow<Double>
        get() = context.dataStore.data.map { it[btcPriceKey] ?: 45000.0 }

    val syncEnableFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[syncEnableKey] == true
        }

    val isShowNfcUniversal: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[isShowNfcUniversalKey] != false
        }

    val isShowHealthCheckReminderIntro: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[getShowHealthCheckReminderIntroKey()] != false
        }

    val turnOnNotificationFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[turnOnNotificationKey] != false
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
            it[hideUpsellBannerKey] == true
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
            ) ?: WalletSecuritySetting.DEFAULT
        }

    val homeDisplaySetting: Flow<HomeDisplaySetting>
        get() = context.dataStore.data.map {
            gson.fromJson(
                it[homeDisplaySettingKey],
                HomeDisplaySetting::class.java
            ) ?: HomeDisplaySetting()
        }

    val localCurrencyFlow: Flow<String>
        get() = context.dataStore.data.map {
            it[getLocalCurrencyKey()] ?: USD_CURRENCY
        }

    val biometricConfig: Flow<BiometricConfig>
        get() = context.dataStore.data.map {
            gson.fromJson(
                it[biometricConfigKey],
                BiometricConfig::class.java
            ) ?: BiometricConfig.DEFAULT
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

    suspend fun setHomeDisplaySetting(config: String) {
        context.dataStore.edit {
            it[homeDisplaySettingKey] = config
        }
    }

    suspend fun setBiometricConfig(config: String) {
        context.dataStore.edit {
            it[biometricConfigKey] = config
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

    suspend fun setGroupWalletBackUpBannerKey(keys: Set<String>) {
        context.dataStore.edit {
            it[groupWalletBackupBannerKeysPreferenceKey] = keys
        }
    }

    val groupWalletBackUpBannerKeys: Flow<Set<String>>
        get() = context.dataStore.data.map {
            it[groupWalletBackupBannerKeysPreferenceKey] ?: emptySet()
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
            it[securityQuestionKey] == true
        }

    val useLargeFontHomeBalancesFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[useLargeFontHomeBalances] == true
        }

    val displayTotalBalanceFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[displayTotalBalance] == true
        }

    val antiFeeSnipingFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[antiFeeSniping] == true
        }

    val currentStep: Flow<MembershipStep?>
        get() = context.dataStore.data.map {
            it[currentStepKey]?.let { index -> MembershipStep.entries[index] }
        }

    val defaultFee: Flow<Int>
        get() = context.dataStore.data.map {
            it[defaultFeeKey] ?: FeeRate.ECONOMY.ordinal
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
            it[showNewPortalKey] != false
        }.first()
    }

    suspend fun setShowPortal(show: Boolean) {
        context.dataStore.edit {
            it[showNewPortalKey] = show
        }
    }

    suspend fun setReferrerCode(code: String) {
        context.dataStore.edit {
            it[getReferrerCodeKey()] = code
        }
    }

    val referralCodeFlow: Flow<String>
        get() = context.dataStore.data.map {
            it[getReferrerCodeKey()].orEmpty()
        }

    suspend fun setCampaign(campaign: String, email: String) {
        context.dataStore.edit {
            it[getCampaignKey(email)] = campaign
        }
    }

    fun getCampaignFlow(email: String): Flow<String> {
        return context.dataStore.data.map {
            it[getCampaignKey(email)].orEmpty()
        }
    }

    suspend fun setLastCloseApp(time: Long) {
        context.dataStore.edit {
            it[lastCloseAppKey] = time
        }
    }

    suspend fun setCustomPinConfig(decoyPin: String, isEnable: Boolean) {
        context.dataStore.edit {
            it[booleanPreferencesKey("decoy_pin_${decoyPin}")] = isEnable
        }
    }

    fun getCustomPinConfig(decoyPin: String): Flow<Boolean> {
        return context.dataStore.data.map {
            it[booleanPreferencesKey("decoy_pin_${decoyPin}")] != false
        }
    }

    val isDarkMode: Flow<Boolean?>
        get() = context.dataStore.data.map {
            it[isDarkModeKey]
        }

    suspend fun setDarkMode(isDarkMode: Boolean?) {
        if (isDarkMode != null) {
            context.dataStore.edit {
                it[isDarkModeKey] = isDarkMode
            }
        } else {
            context.dataStore.edit {
                it.remove(isDarkModeKey)
            }
        }
    }

    suspend fun setDefaultFee(fee: Int) {
        context.dataStore.edit {
            it[defaultFeeKey] = fee
        }
    }

    suspend fun setAntiFeeSniping(isEnable: Boolean) {
        context.dataStore.edit {
            it[antiFeeSniping] = isEnable
        }
    }

    val lastCloseApp: Flow<Long>
        get() = context.dataStore.data.map {
            it[lastCloseAppKey] ?: 0L
        }

    suspend fun setTaprootFeeSelection(info: TaprootFeeSelectionSetting) {
        context.dataStore.edit {
            it[taprootFeeSelectionKey] = gson.toJson(info)
        }
    }

    val taprootFeeSelection: Flow<TaprootFeeSelectionSetting>
        get() = context.dataStore.data.map {
            gson.fromJson(
                it[taprootFeeSelectionKey],
                TaprootFeeSelectionSetting::class.java
            ) ?: TaprootFeeSelectionSetting()
        }

    val firstChatId: Flow<String>
        get() = context.dataStore.data.map {
            it[firstChatIdKey].orEmpty()
        }

    val hasWalletInGuestMode: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[hasWalletInGuestModeKey] == true
        }

    suspend fun setFirstChatId(chatId: String, isForce: Boolean) {
        context.dataStore.edit { preferences ->
            if (preferences[firstChatIdKey].isNullOrEmpty() || isForce) {
                preferences[firstChatIdKey] = chatId
            }
        }
    }

    suspend fun setHasWalletInGuestMode(hasWallet: Boolean) {
        context.dataStore.edit {
            it[hasWalletInGuestModeKey] = hasWallet
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