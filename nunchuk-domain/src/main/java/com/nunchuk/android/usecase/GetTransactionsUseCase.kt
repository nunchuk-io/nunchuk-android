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

package com.nunchuk.android.usecase

import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetTransactionsUseCase {
    fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>): Flow<List<TransactionExtended>>
}

internal class GetTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionsUseCase {

    override fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>) = flow {
        emit(eventIds.mapNotNull {
            val initEventId = it.first
            val isReceive = it.second
            getTransaction(walletId = walletId, initEventId = initEventId, isReceive = isReceive)
        })
    }.catch {
        CrashlyticsReporter.recordException(it)
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    private fun getTransaction(walletId: String, initEventId: String, isReceive: Boolean): TransactionExtended? {
        try {
            val txId = if (isReceive) {
                nativeSdk.getTransactionId(initEventId)
            } else {
                nativeSdk.getRoomTransaction(initEventId).txId
            }
            if (txId.isEmpty()) return null
            val chainTip = nativeSdk.getChainTip()
            val tx = nativeSdk.getTransaction(walletId, txId = txId)
            return TransactionExtended(walletId = walletId, initEventId, tx.copy(height = tx.getConfirmations(chainTip)))
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            return null
        }
    }
}