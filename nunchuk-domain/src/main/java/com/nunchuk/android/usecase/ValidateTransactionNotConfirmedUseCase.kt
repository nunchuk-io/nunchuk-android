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
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.TransactionStatus
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ValidateTransactionNotConfirmedUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<ValidateTransactionNotConfirmedUseCase.Params, Unit>(dispatcher) {

    override suspend fun execute(parameters: Params) {
        // Only validate if replaceTxId is provided
        if (parameters.replaceTxId.isNotEmpty()) {
            try {
                val tx = nativeSdk.getTransaction(parameters.walletId, parameters.replaceTxId)
                if (tx.status == TransactionStatus.CONFIRMED) {
                    throw TransactionAlreadyConfirmedException()
                }
            } catch (e: TransactionAlreadyConfirmedException) {
                // Re-throw our specific exception
                throw e
            } catch (e: Exception) {
                // Silently catch all other exceptions
            }
        }
    }

    data class Params(
        val walletId: String,
        val replaceTxId: String
    )
} 