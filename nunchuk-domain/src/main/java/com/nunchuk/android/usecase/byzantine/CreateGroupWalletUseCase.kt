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

package com.nunchuk.android.usecase.byzantine

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.GroupStatus
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateGroupWalletUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val userWalletRepository: PremiumWalletRepository,
) : UseCase<CreateGroupWalletUseCase.Param, Wallet>(dispatcher) {
    override suspend fun execute(parameters: Param): Wallet {
        val wallet = userWalletRepository.createGroupWallet(
            groupId = parameters.groupId,
            name = parameters.name,
            primaryMembershipId = parameters.primaryMembershipId,
            sendBsmsEmail = parameters.sendBsmsEmail
        )
        userWalletRepository.updateGroupStatus(parameters.groupId, GroupStatus.ACTIVE.name)
        return wallet
    }

    data class Param(
        val groupId: String,
        val name: String,
        val primaryMembershipId: String?,
        val sendBsmsEmail: Boolean = false
    )
}