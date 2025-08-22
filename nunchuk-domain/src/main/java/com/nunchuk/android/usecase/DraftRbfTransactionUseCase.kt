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
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

class DraftRbfTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val validateTransactionNotConfirmedUseCase: ValidateTransactionNotConfirmedUseCase,
) : UseCase<DraftRbfTransactionUseCase.Params, Transaction>(ioDispatcher) {
    override suspend fun execute(parameters: Params): Transaction {
        Timber.d("CongHai parameters: $parameters")
        if (parameters.isValidateTransactionNotConfirmed) {
            // Validate that the transaction being replaced is not already confirmed
            validateTransactionNotConfirmedUseCase(
                ValidateTransactionNotConfirmedUseCase.Params(
                    walletId = parameters.walletId,
                    replaceTxId = parameters.replaceTxId
                )
            ).onFailure { exception -> throw exception }
        }
        return nativeSdk.draftRbfTransaction(
            walletId = parameters.walletId,
            feeRate = parameters.feeRate,
            replaceTxId = parameters.replaceTxId,
            useScriptPath = parameters.useScriptPath,
            signingPath = parameters.signingPath
        )
    }

    data class Params(
        val walletId: String,
        val feeRate: Amount,
        val replaceTxId: String,
        val isValidateTransactionNotConfirmed: Boolean = false,
        val useScriptPath: Boolean = false,
        val signingPath: SigningPath? = null,
    )
} 