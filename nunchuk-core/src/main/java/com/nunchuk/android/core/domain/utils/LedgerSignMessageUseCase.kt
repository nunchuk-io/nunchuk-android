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
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.LedgerTransport
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Starts the "sign message" command on the Ledger session for the given derivation
 * path and message. Used for both plain message signing and health checks. Returns
 * the first [LedgerStep].
 */
class LedgerSignMessageUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<LedgerSignMessageUseCase.Param, LedgerStep>(dispatcher) {

    override suspend fun execute(parameters: Param): LedgerStep {
        return nunchukNativeSdk.ledgerSignMessage(
            sessionId = parameters.sessionId,
            transport = parameters.transport,
            derivationPath = parameters.derivationPath,
            message = parameters.message
        )
    }

    data class Param(
        val sessionId: String,
        val transport: LedgerTransport,
        val derivationPath: String,
        val message: String
    )
}
