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

package com.nunchuk.android.repository

import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    fun uploadBackupKey(
        step: MembershipStep,
        keyName: String,
        keyType: String,
        xfp: String,
        cardId: String,
        filePath: String,
        isAddNewKey: Boolean,
        plan: MembershipPlan,
        groupId: String,
        newIndex: Int
    ): Flow<KeyUpload>

    suspend fun setKeyVerified(
        groupId: String,
        masterSignerId: String,
        isAppVerify: Boolean,
    )

    suspend fun initReplaceKey(
        groupId: String?,
        walletId: String,
        xfp: String,
    )

    suspend fun cancelReplaceKey(
        groupId: String?,
        walletId: String,
        xfp: String,
    )

    suspend fun finalizeReplaceWallet(
        groupId: String?,
        walletId: String,
    )

    suspend fun replaceKey(
        groupId: String?,
        walletId: String,
        signer: SingleSigner,
        xfp: String,
    )
}