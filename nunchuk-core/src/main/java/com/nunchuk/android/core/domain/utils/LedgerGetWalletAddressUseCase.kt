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
import com.nunchuk.android.model.LedgerStep
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.LedgerTransport
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Starts the "get wallet address" command on the Ledger session so the address can
 * be verified on-device. Requires the wallet HMAC. On completion the result string
 * is the address shown by the device.
 */
class LedgerGetWalletAddressUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<LedgerGetWalletAddressUseCase.Param, LedgerStep>(dispatcher) {

    override suspend fun execute(parameters: Param): LedgerStep {
        return nunchukNativeSdk.ledgerGetWalletAddress(
            sessionId = parameters.sessionId,
            transport = parameters.transport,
            wallet = parameters.wallet,
            hmac = parameters.hmac,
            addressIndex = parameters.addressIndex,
            change = parameters.change
        )
    }

    data class Param(
        val sessionId: String,
        val transport: LedgerTransport,
        val wallet: Wallet,
        val hmac: String,
        val addressIndex: Int,
        val change: Boolean = false
    )
}
