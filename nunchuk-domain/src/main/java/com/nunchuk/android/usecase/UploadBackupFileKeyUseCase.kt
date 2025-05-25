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

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UploadBackupFileKeyUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository,
    private val userWalletRepository: PremiumWalletRepository,
) : FlowUseCase<UploadBackupFileKeyUseCase.Param, KeyUpload>(dispatcher) {
    override fun execute(parameters: Param): Flow<KeyUpload> =
        repository.uploadBackupKey(
            step = parameters.step,
            keyName = parameters.keyName,
            keyType = parameters.keyType,
            xfp = parameters.xfp,
            cardId = parameters.cardId,
            filePath = parameters.filePath,
            isAddNewKey = parameters.isAddNewKey,
            plan = parameters.plan,
            groupId = parameters.groupId,
            newIndex = parameters.signerIndex,
            isRequestAddKey = parameters.isRequestAddKey,
            existingColdCard = parameters.existingColdCard
        ).onCompletion {
            withContext(NonCancellable) {
                runCatching {
                    userWalletRepository.cancelRequestIdIfNeed(parameters.groupId, parameters.step)
                }
            }
        }

    data class Param(
        val step: MembershipStep,
        val keyName: String,
        val keyType: String,
        val xfp: String,
        val cardId: String,
        val filePath: String,
        val isAddNewKey: Boolean,
        val plan: MembershipPlan,
        val groupId: String,
        val signerIndex: Int,
        val existingColdCard: SingleSigner?,
        val isRequestAddKey: Boolean = true
    )
}