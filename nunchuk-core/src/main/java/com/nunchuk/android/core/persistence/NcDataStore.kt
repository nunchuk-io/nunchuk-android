/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.toMembershipPlan
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
) {
    private val btcPriceKey = doublePreferencesKey("btc_price")
    private val turnOnNotificationKey = booleanPreferencesKey("turn_on_notification")
    private val syncEnableKey = booleanPreferencesKey("sync_enable")
    private val isShowNfcUniversalKey = booleanPreferencesKey("show_nfc_universal")

    /**
     * Assisted wallet local id
     */
    private val assistedWalletLocalIdKey = stringPreferencesKey("assisted_wallet_local_id")

    /**
     * Plan of current assisted wallet
     */
    private val assistedWalletPlanKey = stringPreferencesKey("assisted_wallet_plan")

    /**
     * Current membership plan key
     */
    private val membershipPlanKey = intPreferencesKey("membership_plan")

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

    val assistedWalletId: Flow<String>
        get() = context.dataStore.data.map {
            it[assistedWalletLocalIdKey].orEmpty()
        }

    val assistedWalletPlan: Flow<MembershipPlan>
        get() = context.dataStore.data.map {
            it[assistedWalletPlanKey].toMembershipPlan()
        }

    val membershipPlan: Flow<MembershipPlan>
        get() = context.dataStore.data.map {
            val ordinal = it[membershipPlanKey] ?: 0
            MembershipPlan.values()[ordinal]
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

    suspend fun setAssistedWalletId(id: String) {
        context.dataStore.edit { settings ->
            settings[assistedWalletLocalIdKey] = id
        }
    }

    suspend fun setAssistedWalletPlan(plan: String) {
        context.dataStore.edit { settings ->
            settings[assistedWalletPlanKey] = plan
        }
    }

    suspend fun setMembershipPlan(plan: MembershipPlan) {
        context.dataStore.edit {
            it[membershipPlanKey] = plan.ordinal
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(syncEnableKey)
            it.remove(turnOnNotificationKey)
            it.remove(assistedWalletLocalIdKey)
        }
    }
}