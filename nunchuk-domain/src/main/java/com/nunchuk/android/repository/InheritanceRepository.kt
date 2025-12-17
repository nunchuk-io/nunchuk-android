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

import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.type.SignerTag
import kotlinx.coroutines.flow.Flow

interface InheritanceRepository {
    suspend fun inheritanceClaimingInit(magic: String): InheritanceClaimingInit

    suspend fun downloadWallet(
        magic: String,
        keys: List<SingleSigner>
    ): WalletServer

    suspend fun isClaimWallet(walletId: String): Boolean

    fun getClaimWalletsFlow(): Flow<Set<String>>

    suspend fun getClaimingWallet(localId: String): WalletServer

    suspend fun requestAddKeyForInheritance(magic: String, signerTags: List<SignerTag>): String

    suspend fun checkKeyAddedForInheritance(
        magic: String,
        requestId: String?
    ): Boolean

    suspend fun deletePendingRequestsByMagic(magic: String)

    suspend fun getAddedKeys(magic: String): Map<String, SingleSigner>

    fun isCurrentUserClaimedWallet(): Flow<Boolean>
}
