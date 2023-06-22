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
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ImportTransactionUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val userWalletRepository: PremiumWalletRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<ImportTransactionUseCase.Param, Transaction>(ioDispatcher) {

    data class Param(
        val walletId: String,
        val filePath: String,
        val isAssistedWallet: Boolean,
        val initEventId: String = "",
        val masterFingerPrint: String = "",
    )

    override suspend fun execute(parameters: Param): Transaction {
        val result = nativeSdk.importTransaction(walletId = parameters.walletId, filePath = parameters.filePath)
        if (parameters.masterFingerPrint.isNotEmpty() && parameters.initEventId.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(parameters.initEventId, parameters.masterFingerPrint)
        }
        if (parameters.isAssistedWallet) {
            runCatching {
                userWalletRepository.createServerTransaction(
                    walletId = parameters.walletId,
                    psbt = result.psbt,
                    note = result.memo
                )
            }
        }
        return result
    }
}
