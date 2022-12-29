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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateSignerUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<CreateSignerUseCase.Params, SingleSigner>(ioDispatcher) {

    override suspend fun execute(parameters: Params): SingleSigner {
        return nativeSdk.createSigner(
            name = parameters.name,
            xpub = parameters.xpub,
            publicKey = "",
            derivationPath = parameters.derivationPath,
            masterFingerprint = parameters.masterFingerprint,
            type = parameters.type
        )
    }

    data class Params(
        val name: String,
        val xpub: String,
        val type: SignerType,
        val derivationPath: String,
        val masterFingerprint: String
    )
}