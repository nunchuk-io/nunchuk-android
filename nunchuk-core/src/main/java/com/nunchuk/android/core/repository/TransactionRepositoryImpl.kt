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

import com.google.gson.Gson
import com.nunchuk.android.core.data.api.TransactionApi
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.repository.TransactionRepository
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionApi: TransactionApi,
    private val gson: Gson,
    private val ncDataStore: NcDataStore,
    applicationScope: CoroutineScope,
) : TransactionRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override suspend fun getFees(): EstimateFeeRates {
        val data = when (chain.value) {
            Chain.MAIN -> transactionApi.getFees()
            Chain.TESTNET -> transactionApi.getTestnetFees()
            Chain.SIGNET -> transactionApi.getSignetFees()
            Chain.REGTEST -> transactionApi.getTestnetFees()
        }
        val fee = EstimateFeeRates(
            priorityRate = data.priorityRate,
            standardRate = data.standardRate,
            economicRate = data.economicRate,
            minimumFee = data.minimumFee
        )

        ncDataStore.setFeeJsonString(gson.toJson(fee))
        return fee
    }

    override suspend fun getLocalFee(): EstimateFeeRates {
        return runCatching {
            gson.fromJson(
                ncDataStore.fee.firstOrNull(),
                EstimateFeeRates::class.java
            )
        }.getOrNull() ?: EstimateFeeRates()
    }
}