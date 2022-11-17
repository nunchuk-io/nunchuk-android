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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ImportTransactionUseCase {
    fun execute(walletId: String, filePath: String, initEventId: String = "", masterFingerPrint: String = ""): Flow<Transaction>
}

internal class ImportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportTransactionUseCase {

    override fun execute(walletId: String, filePath: String, initEventId: String, masterFingerPrint: String): Flow<Transaction> = flow {
        val result = nativeSdk.importTransaction(walletId = walletId, filePath = filePath)
        if (masterFingerPrint.isNotEmpty() && initEventId.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(initEventId, masterFingerPrint)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

}
