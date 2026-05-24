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

package com.nunchuk.android.repository

import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.LiquidNetworkStatus
import com.nunchuk.android.model.TxInput

interface TransactionRepository {
    suspend fun getFees(): EstimateFeeRates

    suspend fun getLiquidFees(): EstimateFeeRates

    suspend fun getLiquidNetworkStatus(): LiquidNetworkStatus

    suspend fun getLocalFee(): EstimateFeeRates
    suspend fun batchTransactions(
        walletId: String,
        groupId: String,
        notes: List<String>,
        psbts: List<String>
    )
    suspend fun randomizeBroadcastBatchTransactions(
        walletId: String,
        groupId: String,
        transactionIds: List<String>,
        days: Int
    )

    /**
     * Save the selected key set index for a taproot transaction.
     */
    suspend fun saveTaprootKeySetSelection(transactionId: String, keySetIndex: Int)

    /**
     * Get the selected key set index for a taproot transaction, or null if not set.
     */
    suspend fun getTaprootKeySetSelection(transactionId: String): Int?

    /**
     * Persist the user-selected coin inputs of the in-flight transaction draft.
     * Keeps Intent extras small to avoid TransactionTooLargeException.
     */
    suspend fun savePendingTxInputs(walletId: String, inputs: List<TxInput>)

    /**
     * Returns the saved inputs only when they belong to [walletId]; otherwise an empty list.
     */
    suspend fun getPendingTxInputs(walletId: String): List<TxInput>

    /**
     * Clears the saved pending transaction inputs, e.g. after a transaction is created.
     */
    suspend fun clearPendingTxInputs()
}