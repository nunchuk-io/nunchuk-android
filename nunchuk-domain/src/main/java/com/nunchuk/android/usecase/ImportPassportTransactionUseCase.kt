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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ImportPassportTransactionUseCase {
    fun execute(walletId: String, qrData: List<String>, initEventId: String = "", masterFingerPrint: String = ""): Flow<Transaction>
}

internal class ImportPassportTransactionUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ImportPassportTransactionUseCase {

    override fun execute(walletId: String, qrData: List<String>, initEventId: String, masterFingerPrint: String): Flow<Transaction> = flow {
        val result = nativeSdk.importPassportTransaction(walletId = walletId, qrData = qrData)
        if (masterFingerPrint.isNotEmpty() && initEventId.isNotEmpty()) {
            nativeSdk.signAirgapTransaction(initEventId, masterFingerPrint)
        }
        emit(result)
    }

}