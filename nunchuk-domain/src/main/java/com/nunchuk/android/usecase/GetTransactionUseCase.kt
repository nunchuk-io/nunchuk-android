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

import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetTransactionUseCase {
    fun execute(
        walletId: String,
        txId: String,
        isAssistedWallet: Boolean
    ): Flow<ExtendedTransaction>
}

internal class GetTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val repository: PremiumWalletRepository,
) : GetTransactionUseCase {

    override fun execute(
        walletId: String,
        txId: String,
        isAssistedWallet: Boolean
    ): Flow<ExtendedTransaction> = flow {
        val chainTip = nativeSdk.getChainTip()
        val tx = nativeSdk.getTransaction(walletId = walletId, txId = txId)
        emit(ExtendedTransaction(transaction = tx.copy(height = tx.getConfirmations(chainTip))))
        if (isAssistedWallet && tx.signers.any { it.value } && tx.status.isPending()) {
            val extendedTransaction = repository.getServerTransaction(walletId, txId)
            val transaction = extendedTransaction.transaction.copy(
                height = extendedTransaction.transaction.getConfirmations(chainTip)
            )
            emit(extendedTransaction.copy(transaction = transaction))
        }
    }

    private fun TransactionStatus.isPending() =
        this == TransactionStatus.PENDING_SIGNATURES || this == TransactionStatus.READY_TO_BROADCAST
}

