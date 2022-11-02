package com.nunchuk.android.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.nunchuk.android.core.domain.data.SyncSetting
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
    private val turnOnNotification = booleanPreferencesKey("turn_on_notification")
    private val syncEnableKey = booleanPreferencesKey("sync_enable")

    val btcPriceFlow: Flow<Double>
        get() = context.dataStore.data.map { it[btcPriceKey] ?: 45000.0 }

    val syncEnableFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[syncEnableKey] ?: false
        }

    val turnOnNotificationFlow: Flow<Boolean>
        get() = context.dataStore.data.map {
            it[turnOnNotification] ?: true
        }

    suspend fun updateTurnOnNotification(turnOn: Boolean) {
        context.dataStore.edit { settings ->
            settings[turnOnNotification] = turnOn
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

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(syncEnableKey)
            it.remove(turnOnNotification)
        }
    }
}