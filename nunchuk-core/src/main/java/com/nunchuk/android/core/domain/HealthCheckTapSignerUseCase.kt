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
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.HealthStatus
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class HealthCheckTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<HealthCheckTapSignerUseCase.Data, HealthStatus>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): HealthStatus {
        return nunchukNativeSdk.healthCheckTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            fingerprint = parameters.fingerprint,
            message = parameters.message,
            signature = parameters.signature,
            path = parameters.path
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val fingerprint: String,
        val message: String,
        val signature: String,
        val path: String
    ) : BaseNfcUseCase.Data(isoDep)
}