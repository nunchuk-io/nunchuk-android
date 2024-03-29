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

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetInheritanceClaimStateUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<GetInheritanceClaimStateUseCase.Param, InheritanceAdditional>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): InheritanceAdditional {
        val userData = userWalletRepository.generateInheritanceClaimStatusUserData(
            magic = parameters.magic,
        )
        val signatures = arrayListOf<String>()
        val singleSigners = arrayListOf<SingleSigner>()
        parameters.signerModels.forEachIndexed { index, signerModel ->
            val signer = nunchukNativeSdk.getSignerFromMasterSigner(
                masterSignerId = signerModel.id, path = parameters.derivationPaths[index]
            )
            val messagesToSign = nunchukNativeSdk.getHealthCheckMessage(userData)
            val signature = nunchukNativeSdk.signHealthCheckMessage(signer, messagesToSign)
            signatures.add(signature)
            singleSigners.add(signer)
        }
        return userWalletRepository.inheritanceClaimStatus(
            userData = userData,
            masterFingerprints = singleSigners.map { it.masterFingerprint },
            signatures = signatures
        )
    }

    data class Param(
        val signerModels: List<SignerModel>,
        val magic: String,
        val derivationPaths: List<String>,
    )
}