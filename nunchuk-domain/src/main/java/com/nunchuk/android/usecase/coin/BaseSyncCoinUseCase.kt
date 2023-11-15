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

package com.nunchuk.android.usecase.coin

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

abstract class BaseSyncCoinUseCase<P : BaseSyncCoinUseCase.Param, R>(
    private val repository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk,
    dispatcher: CoroutineDispatcher
) : UseCase<P, R>(dispatcher) {
    override suspend fun execute(parameters: P): R {
        return run(parameters).also {
            if (parameters.isAssistedWallet) {
                withContext(NonCancellable) {// run in another thread to make it does not blocking current execution
                    runCatching {
                        val data = nunchukNativeSdk.exportCoinControlData(parameters.walletId)
                        if (data.isNotEmpty()) {
                            repository.uploadCoinControlData(parameters.groupId, parameters.walletId, data)
                        }
                    }
                }
            }
        }
    }

    abstract suspend fun run(parameters: P): R

    abstract class Param(
        open val groupId: String?,
        open val walletId: String,
        open val isAssistedWallet: Boolean
    )
}