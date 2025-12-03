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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.repository.ClaimWalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ClaimWalletRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val ncDataStore: NcDataStore,
) : ClaimWalletRepository {
    override suspend fun deleteClaimingWallet(localId: String) {
        runCatching {
            val response = userWalletApiManager.claimWalletApi.deleteClaimingWallet(localId)
            if (response.isSuccess.not()) {
                throw response.error
            }
        }
    }

    override suspend fun isClaimWallet(localId: String): Boolean {
        val claimWallets = ncDataStore.claimWalletsFlow.first()
        return claimWallets.contains(localId)
    }
}

