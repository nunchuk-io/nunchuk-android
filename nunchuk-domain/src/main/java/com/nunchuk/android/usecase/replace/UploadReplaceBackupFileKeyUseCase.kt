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

package com.nunchuk.android.usecase.replace

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.repository.KeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadReplaceBackupFileKeyUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: KeyRepository,
) : FlowUseCase<UploadReplaceBackupFileKeyUseCase.Param, KeyUpload>(dispatcher) {
    override fun execute(parameters: Param): Flow<KeyUpload> =
        repository.uploadReplaceBackupKey(
            replacedXfp = parameters.replacedXfp,
            keyName = parameters.keyName,
            keyType = parameters.keyType,
            xfp = parameters.xfp,
            cardId = parameters.cardId,
            filePath = parameters.filePath,
            isAddNewKey = parameters.isAddNewKey,
            signerIndex = parameters.signerIndex,
            walletId = parameters.walletId,
            groupId = parameters.groupId,
            isRequestReplaceKey = parameters.isRequestReplaceKey,
            existingColdCard = parameters.existingColdCard
        )
    data class Param(
        val replacedXfp: String,
        val keyName: String,
        val keyType: String,
        val xfp: String,
        val cardId: String,
        val filePath: String,
        val isAddNewKey: Boolean,
        val signerIndex: Int,
        val walletId: String,
        val groupId: String,
        val existingColdCard: SingleSigner?,
        val isRequestReplaceKey: Boolean = true
    )
}