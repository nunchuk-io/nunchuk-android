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

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException

abstract class BaseMk4UseCase<P : BaseMk4UseCase.Data>(
    dispatcher: CoroutineDispatcher,
) : UseCase<P, Unit>(dispatcher) {
    final override suspend fun execute(parameters: P) {
        parameters.ndef.connect()
        try {
            if (parameters.ndef.isConnected) {
                val result = executeNfc(parameters)
                if (result.isNotEmpty()) {
                    parameters.ndef.writeNdefMessage(NdefMessage(result))
                }
            } else {
                throw IOException("Can not connect nfc card")
            }
        } catch (e: Throwable) {
            throw e
        } finally {
            runCatching { parameters.ndef.close() }
        }
    }

    protected abstract suspend fun executeNfc(parameters: P): Array<NdefRecord>

    open class Data(val ndef: Ndef)
}