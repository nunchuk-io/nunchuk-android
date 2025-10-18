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
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CalculateRequiredSignaturesInheritanceUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
) : UseCase<CalculateRequiredSignaturesInheritanceUseCase.Param, CalculateRequiredSignatures>(
    dispatcher
) {
    override suspend fun execute(parameters: Param): CalculateRequiredSignatures {
        return userWalletsRepository.calculateRequiredSignaturesInheritance(
            walletId = parameters.walletId,
            note = parameters.note,
            notificationEmails = parameters.notificationEmails,
            notifyToday = parameters.notifyToday,
            activationTimeMilis = parameters.activationTimeMilis,
            bufferPeriodId = parameters.bufferPeriodId,
            action = parameters.action,
            groupId = parameters.groupId,
            notificationPreferences = parameters.notificationPreferences
        )
    }

    class Param(
        val note: String = "",
        val notificationEmails: List<String> = emptyList(),
        val notifyToday: Boolean = false,
        val activationTimeMilis: Long = 0L,
        val walletId: String,
        val bufferPeriodId: String? = null,
        val action: CalculateRequiredSignaturesAction,
        val groupId: String? = null,
        val notificationPreferences: InheritanceNotificationSettings? = null
    )
}