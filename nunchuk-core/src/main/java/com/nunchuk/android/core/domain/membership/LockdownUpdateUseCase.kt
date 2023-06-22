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

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LockdownUpdateUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<LockdownUpdateUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        val authorizations = mutableListOf<String>()
        parameters.signatures.forEach { (masterFingerprint, signature) ->
            val requestToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
            authorizations.add(requestToken)
        }
        return userWalletRepository.lockdownUpdate(
            authorizations = authorizations,
            verifyToken = parameters.verifyToken,
            userData =  parameters.userData,
            securityQuestionToken = parameters.securityQuestionToken
        )
    }

    class Param(
        val signatures: Map<String, String>,
        val userData: String,
        val verifyToken: String,
        val securityQuestionToken: String
    )
}