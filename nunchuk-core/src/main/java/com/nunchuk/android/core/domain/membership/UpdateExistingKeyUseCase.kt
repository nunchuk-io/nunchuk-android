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

package com.nunchuk.android.core.domain.membership

import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateExistingKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<UpdateExistingKeyUseCase.Params, Unit>(dispatcher) {
    override suspend fun execute(parameters: Params) {
        if (parameters.replace) {
            val tapsigner = parameters.serverSigner.tapsigner
            if (tapsigner != null) {
                nunchukNativeSdk.addTapSigner(
                    cardId = tapsigner.cardId,
                    name = parameters.serverSigner.name.orEmpty(),
                    xfp = parameters.serverSigner.xfp.orEmpty(),
                    version = tapsigner.version.orEmpty(),
                    brithHeight = tapsigner.birthHeight,
                    isTestNet = tapsigner.isTestnet,
                    replace = true
                )
            } else {
                nunchukNativeSdk.createSigner(
                    name = parameters.serverSigner.name.orEmpty(),
                    xpub = parameters.serverSigner.xpub.orEmpty(),
                    publicKey = parameters.serverSigner.pubkey.orEmpty(),
                    derivationPath = parameters.serverSigner.derivationPath.orEmpty(),
                    masterFingerprint = parameters.serverSigner.xfp.orEmpty(),
                    type = parameters.serverSigner.type,
                    tags = parameters.serverSigner.tags.mapNotNull { tag -> tag.toSignerTag() },
                    replace = false
                )
            }
        } else {
            userWalletsRepository.updateKeyType(localSigner = parameters.localSigner, serverSigner = parameters.serverSigner)
        }
    }

    class Params(val serverSigner: SignerServer, val localSigner: SingleSigner, val replace: Boolean)
}