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

package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.ElectrumServersDto
import com.nunchuk.android.core.data.model.PriceWrapperResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.GET

internal interface PriceConverterAPI {

    @GET("prices")
    suspend fun getPrices(): Data<PriceWrapperResponse>

    @GET("/v1.1/forex/rates")
    suspend fun getForexRates(): HashMap<String, Double>

    @GET("/v1.1/forex/currencies")
    suspend fun getForexCurrencies(): HashMap<String, String>

    @GET("/v1.1/app/electrum")
    suspend fun getElectrumServers(): Data<ElectrumServersDto>
}