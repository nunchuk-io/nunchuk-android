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

import com.nunchuk.android.core.data.api.PriceConverterAPI
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.ElectrumServers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface BtcRepository {
    suspend fun getRemotePrice()
    fun getLocalPrice(): Flow<Double>
    suspend fun getForexCurrencies(): HashMap<String, String>

    suspend fun getElectrumServers(): ElectrumServers
}

internal class BtcRepositoryImpl @Inject constructor(
    private val priceConverterAPI: PriceConverterAPI,
    private val ncDataStore: NcDataStore
) : BtcRepository {

    override suspend fun getRemotePrice() {
        coroutineScope {
            val ratesResult = async { priceConverterAPI.getForexRates() }
            val btcPriceResult = async { priceConverterAPI.getPrices() }
            val rates = ratesResult.await()
            val btcPrice = btcPriceResult.await().data.prices.btc?.usd ?: 0.0
            val localCurrency = ncDataStore.localCurrencyFlow.first()
            val rate = rates[localCurrency] ?: 0.0
            ncDataStore.updateBtcPrice(rate * btcPrice)
        }
    }

    override fun getLocalPrice(): Flow<Double> = ncDataStore.btcPriceFlow

    override suspend fun getForexCurrencies(): HashMap<String, String> {
        return priceConverterAPI.getForexCurrencies()
    }

    override suspend fun getElectrumServers(): ElectrumServers {
        val dto = priceConverterAPI.getElectrumServers().data
        return ElectrumServers(
            mainnet = dto.mainnet.map { it.url },
            testnet = dto.testnet.map { it.url },
            signet = dto.signet.map { it.url }
        )
    }
}