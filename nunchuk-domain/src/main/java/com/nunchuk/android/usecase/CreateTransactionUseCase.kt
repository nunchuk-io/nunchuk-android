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
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val repository: PremiumWalletRepository,
    private val validateTransactionNotConfirmedUseCase: ValidateTransactionNotConfirmedUseCase,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<CreateTransactionUseCase.Param, Transaction>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Transaction {
        // Validate that the transaction being replaced is not already confirmed
        validateTransactionNotConfirmedUseCase(
            ValidateTransactionNotConfirmedUseCase.Params(
                walletId = parameters.walletId,
                replaceTxId = parameters.replaceTxId
            )
        )
        
        val transaction = nativeSdk.createTransaction(
            walletId = parameters.walletId,
            outputs = parameters.outputs,
            memo = parameters.memo,
            inputs = parameters.inputs,
            feeRate = parameters.feeRate,
            subtractFeeFromAmount = parameters.subtractFeeFromAmount,
            replaceTxId = parameters.replaceTxId,
            antiFeeSniping = parameters.antiFeeSniping,
            useScriptPath = parameters.useScriptPath
        )
        if (parameters.isAssistedWallet) {
            try {
                repository.createServerTransaction(
                    parameters.groupId,
                    parameters.walletId,
                    transaction.psbt,
                    transaction.memo,
                )
            } catch (e: Exception) {
                throw e
            }
        }
        return transaction
    }

    data class Param(
        val groupId: String?,
        val walletId: String,
        val outputs: Map<String, Amount>,
        val isAssistedWallet: Boolean,
        val memo: String = "",
        val inputs: List<TxInput> = emptyList(),
        val feeRate: Amount = Amount(-1),
        val subtractFeeFromAmount: Boolean = false,
        val replaceTxId : String = "",
        val antiFeeSniping: Boolean,
        val useScriptPath: Boolean = false,
    )
}