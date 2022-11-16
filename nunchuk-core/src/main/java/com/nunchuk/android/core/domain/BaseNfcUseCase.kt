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

package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.TapProtocolException
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import java.io.IOException

abstract class BaseNfcUseCase<P : BaseNfcUseCase.Data, R>(
    dispatcher: CoroutineDispatcher,
    private val waitAutoCardUseCase: WaitAutoCardUseCase
) : UseCase<P, R>(dispatcher) {
    final override suspend fun execute(parameters: P): R {
        parameters.isoDep.timeout = NFC_CARD_TIMEOUT
        parameters.isoDep.connect()
        try {
            if (parameters.isoDep.isConnected) {
                if (waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] == true) {
                    val result = waitAutoCardUseCase(parameters.isoDep)
                    if (result.isFailure) {
                        Timber.d("waitAutoCardUseCase ${result.exceptionOrNull()?.message.orEmpty()}")
                    }
                    result.isSuccess
                }
                val result = executeNfc(parameters)
                if (isAutoRemoveRateLimit) {
                    waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] = false
                }
                return result
            }
        } catch (e: Throwable) {
            if (e is NCNativeException && e.message.contains(TapProtocolException.RATE_LIMIT.toString())) {
                Timber.d("NFC Rate limit")
                waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] = true
            } else if (e is NCNativeException
                && e.message.contains(TapProtocolException.BAD_AUTH.toString())
                && waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] == true
            ) {
                // if bad_auth in case tap signer still rate limit, should throw rate_limit instead
                throw NCNativeException(TapProtocolException.RATE_LIMIT.toString())
            }
            throw e
        } finally {
            runCatching { parameters.isoDep.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    protected abstract suspend fun executeNfc(parameters: P): R

    open class Data(val isoDep: IsoDep)

    protected open val isAutoRemoveRateLimit: Boolean = true
}