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

package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignTapSignerPsbtUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<SignTapSignerPsbtUseCase.Data, Transaction>(
    dispatcher,
    waitAutoCardUseCase
) {

    override suspend fun executeNfc(parameters: Data): Transaction {
        return nunchukNativeSdk.signTapSignerPsbt(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            signers = parameters.signers,
            psbt = parameters.psbt,
            subAmount = parameters.subAmount,
            feeRate = parameters.feeRate,
            fee = parameters.fee,
            subtractFeeFromAmount = parameters.subtractFeeFromAmount
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val signers: List<SingleSigner>,
        val psbt: String,
        val subAmount: String,
        val feeRate: String,
        val fee: String,
        val subtractFeeFromAmount: Boolean = true
    ) : BaseNfcUseCase.Data(isoDep)
}
