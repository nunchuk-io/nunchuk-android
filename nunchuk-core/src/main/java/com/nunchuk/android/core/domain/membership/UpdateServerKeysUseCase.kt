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
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateServerKeysUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<UpdateServerKeysUseCase.Param, KeyPolicy>(dispatcher) {
    override suspend fun execute(parameters: Param) : KeyPolicy {
        return userWalletsRepository.updateServerKeys(
            signatures = parameters.signatures,
            keyIdOrXfp = parameters.keyIdOrXfp,
            derivationPath = parameters.derivationPath,
            token = parameters.token,
            body = parameters.body,
            securityQuestionToken = parameters.securityQuestionToken
        )
    }

    data class Param(
        val body: String,
        val keyIdOrXfp: String,
        val derivationPath: String,
        val signatures: Map<String, String> = emptyMap(),
        val token: String,
        val securityQuestionToken: String = "",
    )
}