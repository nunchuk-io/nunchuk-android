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

import androidx.annotation.Keep
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class VerifiedPasswordTokenUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<VerifiedPasswordTokenUseCase.Param, String?>(dispatcher) {
    override suspend fun execute(parameters: Param): String? {
        return userWalletsRepository.verifiedPasswordToken(
            targetAction = parameters.targetAction,
            password = parameters.password
        )
    }

    class Param(val targetAction: String, val password: String)
}

@Keep
enum class VerifiedPasswordTargetAction {
    EMERGENCY_LOCKDOWN,
    DOWNLOAD_KEY_BACKUP,
    UPDATE_SECURITY_QUESTIONS,
    UPDATE_INHERITANCE_PLAN,
    UPDATE_SERVER_KEY,
    DELETE_WALLET,
    PROTECT_WALLET,
    EDIT_GROUP_MEMBERS
}