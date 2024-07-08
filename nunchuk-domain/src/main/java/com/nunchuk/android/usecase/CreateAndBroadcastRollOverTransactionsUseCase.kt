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

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.DraftRollOverTransaction
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateAndBroadcastRollOverTransactionsUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val transactionRepository: TransactionRepository
) : UseCase<CreateAndBroadcastRollOverTransactionsUseCase.Param, List<Transaction>?>(dispatcher) {
    override suspend fun execute(parameters: Param): List<Transaction>? {
        val transactions = nunchukNativeSdk.createRollOverTransactions(
            walletId = parameters.oldWalletId,
            newWalletId = parameters.newWalletId,
            tags = parameters.tags,
            collections = parameters.collections,
            feeRate = parameters.feeRate
        )
        if (transactions.isEmpty()) return null
        transactionRepository.batchTransactions(
            walletId = parameters.oldWalletId,
            groupId = parameters.groupId,
            notes = emptyList(),
            psbts = transactions.map { it.psbt },
        )
        if (parameters.randomizeBroadcast) {
            transactionRepository.randomizeBroadcastBatchTransactions(
                walletId = parameters.oldWalletId,
                groupId = parameters.groupId,
                days = parameters.days,
                transactionIds = transactions.map { it.txId }
            )
        }
        return transactions
    }

    class Param(
        val newWalletId: String,
        val oldWalletId: String,
        val tags: List<CoinTag>,
        val collections: List<CoinCollection>,
        val feeRate: Amount,
        val days: Int,
        val groupId: String,
        val randomizeBroadcast: Boolean
    )
}