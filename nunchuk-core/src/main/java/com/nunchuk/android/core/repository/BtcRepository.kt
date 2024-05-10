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
import com.nunchuk.android.model.ElectrumServer
import com.nunchuk.android.model.ElectrumServers
import com.nunchuk.android.model.RemoteElectrumServer
import com.nunchuk.android.persistence.dao.ElectrumServerDao
import com.nunchuk.android.persistence.entity.ElectrumServerEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface BtcRepository {
    suspend fun getRemotePrice()
    fun getLocalPrice(): Flow<Double>
    suspend fun getForexCurrencies(): HashMap<String, String>
    suspend fun getElectrumServers(): ElectrumServers
    fun getLocalElectrumServers(): Flow<List<ElectrumServer>>
    suspend fun addElectrumServer(server: ElectrumServer)
    suspend fun removeElectrumServer(ids: List<Long>)
}

internal class BtcRepositoryImpl @Inject constructor(
    private val priceConverterAPI: PriceConverterAPI,
    private val ncDataStore: NcDataStore,
    private val electrumServerDao: ElectrumServerDao
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
            mainnet = dto.mainnet.map { RemoteElectrumServer(name = it.name, url = it.url) },
            testnet = dto.testnet.map { RemoteElectrumServer(name = it.name, url = it.url) },
            signet = dto.signet.map { RemoteElectrumServer(name = it.name, url = it.url) }
        )
    }

    override fun getLocalElectrumServers(): Flow<List<ElectrumServer>> {
        return electrumServerDao.getAll()
            .map {
                it.map { entity ->
                    ElectrumServer(
                        id = entity.id,
                        url = entity.url,
                        chain = entity.chain
                    )
                }
            }
    }

    override suspend fun addElectrumServer(server: ElectrumServer) {
        electrumServerDao.insert(
            ElectrumServerEntity(
                id = server.id,
                url = server.url,
                chain = server.chain
            )
        )
    }

    override suspend fun removeElectrumServer(ids: List<Long>) {
        electrumServerDao.deleteByIds(ids)
    }
}