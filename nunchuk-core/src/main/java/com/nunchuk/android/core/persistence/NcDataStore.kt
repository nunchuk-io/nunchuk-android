package com.nunchuk.android.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val NAME = "nc_data_store"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = NAME)

@Singleton
class NcDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val btcPriceKey = doublePreferencesKey("btc_price")

    val btcPriceFlow: Flow<Double>
        get() = context.dataStore.data.map { it[btcPriceKey] ?: 45000.0 }

    suspend fun updateBtcPrice(price: Double) {
        context.dataStore.edit { settings ->
            settings[btcPriceKey] = price
        }
    }
}