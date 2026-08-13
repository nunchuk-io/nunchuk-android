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

package com.nunchuk.android.core.domain.utils

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Turns a signature produced outside the SDK — a Ledger signing over BLE/USB — into the same
 * [SignedMessage] the in-app signers return: [signer]'s address plus the RFC2440 block that is
 * copied/exported for proof of address.
 */
class GetSignedMessageUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetSignedMessageUseCase.Param, SignedMessage>(dispatcher) {

    override suspend fun execute(parameters: Param): SignedMessage {
        return nunchukNativeSdk.getSignedMessage(
            signer = parameters.signer,
            message = parameters.message,
            signature = parameters.signature
        )
    }

    data class Param(
        val signer: SingleSigner,
        val message: String,
        val signature: String
    )
}
