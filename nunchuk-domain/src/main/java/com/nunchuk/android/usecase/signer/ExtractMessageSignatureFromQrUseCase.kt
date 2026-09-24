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

package com.nunchuk.android.usecase.signer

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * The signature out of a device's QR reply, whichever shape it takes: a bare base64 compact
 * signature, an armored signed-message block, or BBQR frames. Errors when the data is neither.
 */
class ExtractMessageSignatureFromQrUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<List<String>, String>(dispatcher) {
    override suspend fun execute(parameters: List<String>): String {
        return nativeSdk.extractMessageSignatureFromQr(qrData = parameters)
    }
}
