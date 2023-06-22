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

package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetDummyTxFromPsbt @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
     @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<GetDummyTxFromPsbt.Param, Transaction>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Transaction {
        return nativeSdk.getDummyTx(parameters.walletId, parameters.psbt)
    }

    data class Param(val walletId: String, val psbt: String)
}