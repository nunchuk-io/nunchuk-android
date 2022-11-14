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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.api.PriceConverterAPI
import com.nunchuk.android.core.data.model.PriceResponse
import com.nunchuk.android.core.persistence.NcDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface BtcPriceRepository {
    fun getRemotePrice(): Flow<PriceResponse>
    fun getLocalPrice(): Flow<Double>
}

internal class BtcPriceRepositoryImpl @Inject constructor(
    private val priceConverterAPI: PriceConverterAPI,
    private val ncDataStore: NcDataStore
) : BtcPriceRepository {

    override fun getRemotePrice(
    ) = flow {
        emit(
            priceConverterAPI.getPrices()
        )
    }.map {
        it.data.prices.btc?.usd?.let { priceUsd ->
            ncDataStore.updateBtcPrice(priceUsd)
        }
        it.data.prices
    }

    override fun getLocalPrice(): Flow<Double> = ncDataStore.btcPriceFlow
}