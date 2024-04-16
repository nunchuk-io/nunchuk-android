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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangeKeyTypeUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk
) : UseCase<ChangeKeyTypeUseCase.Params, Unit>(
    ioDispatcher
) {

    override suspend fun execute(parameters: Params) {
        val singleSigner = parameters.singleSigner.copy(
            tags = parameters.signerTag?.let { listOf(it) } ?: emptyList()
        )
        nativeSdk.createSigner(
            name = singleSigner.name,
            xpub = singleSigner.xpub,
            publicKey = singleSigner.publicKey,
            derivationPath = singleSigner.derivationPath,
            masterFingerprint = singleSigner.masterFingerprint,
            type = singleSigner.type,
            tags = singleSigner.tags,
            replace = true
        )
    }

    data class Params(
        val singleSigner: SingleSigner,
        val signerTag: SignerTag? = null,
    )
}