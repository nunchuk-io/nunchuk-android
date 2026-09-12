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

package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetKeyVerifiedUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository
) : UseCase<SetKeyVerifiedUseCase.Param, Unit>(dispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.setKeyVerified(
            groupId = parameters.groupId,
            masterSignerId = parameters.masterSignerId,
            verifyType = parameters.verifyType,
            verificationMethod = parameters.verificationMethod,
        )
    }

    data class Param(
        val groupId: String,
        val masterSignerId: String,
        val verifyType: VerifyType,
        /**
         * Which sharing method of an off-chain inheritance key this verification covers. The two
         * artifacts of a "do both" key are tracked apart server-side, so a verification that does
         * not name its method would resolve the wrong one. Null everywhere else.
         */
        val verificationMethod: ClaimOption? = null,
    )
}