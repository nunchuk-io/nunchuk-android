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
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Feeds bytes received from the Ledger device (BLE / USB) back into the session and
 * returns the next [LedgerStep]. Only call with bytes actually received from the
 * device — never for write failures.
 */
class LedgerOnDataUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<LedgerOnDataUseCase.Param, LedgerStep>(dispatcher) {

    override suspend fun execute(parameters: Param): LedgerStep {
        return nunchukNativeSdk.ledgerOnData(
            sessionId = parameters.sessionId,
            data = parameters.data
        )
    }

    data class Param(
        val sessionId: String,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Param) return false
            return sessionId == other.sessionId && data.contentEquals(other.data)
        }

        override fun hashCode(): Int = 31 * sessionId.hashCode() + data.contentHashCode()
    }
}
