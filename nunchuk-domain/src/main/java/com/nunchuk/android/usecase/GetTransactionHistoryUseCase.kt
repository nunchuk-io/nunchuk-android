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

package com.nunchuk.android.usecase

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface GetTransactionHistoryUseCase {
    fun execute(walletId: String, count: Int = 1000, skip: Int = 0): Flow<List<Transaction>>
}

internal class GetTransactionHistoryUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionHistoryUseCase {

    override fun execute(walletId: String, count: Int, skip: Int) = flow {
        val chainTip = nativeSdk.getChainTip()
        val transactions = nativeSdk.getTransactionHistory(
            walletId = walletId,
            count = count,
            skip = skip
        ).map { it.copy(height = it.getConfirmations(chainTip)) }
        emit(transactions)
    }.catch {
        CrashlyticsReporter.recordException(it)
        emit(emptyList())
    }.flowOn(Dispatchers.IO)
}

fun Transaction.getConfirmations(chainTip: Int) = if (chainTip > 0 && height > 0 && chainTip >= height) {
    (chainTip - height + 1)
} else {
    0
}
