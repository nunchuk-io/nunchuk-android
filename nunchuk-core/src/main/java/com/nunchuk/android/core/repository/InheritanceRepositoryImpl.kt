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

import com.nunchuk.android.core.data.model.InheritanceClaimingDownloadWalletRequest
import com.nunchuk.android.core.data.model.InheritanceClaimingInitRequest
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.ServerSignerMapper
import com.nunchuk.android.core.mapper.toInheritanceClaimingInit
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.InheritanceRepository
import com.nunchuk.android.type.SignerTag
import javax.inject.Inject

internal class InheritanceRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val serverSignerMapper: ServerSignerMapper,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : InheritanceRepository {

    override suspend fun inheritanceClaimingInit(magic: String): InheritanceClaimingInit {
        val request = InheritanceClaimingInitRequest(magic = magic)
        val response = userWalletApiManager.claimInheritanceApi.inheritanceClaimingInit(request)

        return response.data.toInheritanceClaimingInit()
    }

    override suspend fun downloadWallet(
        magic: String,
        keys: List<SingleSigner>
    ): WalletServer {
        val keyDtos = keys.map { singleSigner ->
            val isInheritanceKey = singleSigner.tags.contains(SignerTag.INHERITANCE)
            serverSignerMapper(singleSigner, isInheritanceKey)
        }
        val request = InheritanceClaimingDownloadWalletRequest(
            magic = magic,
            keys = keyDtos
        )
        val response =
            userWalletApiManager.claimInheritanceApi.inheritanceClaimingDownloadWallet(request)
        val walletServer = response.data.wallet
            ?: throw IllegalStateException("Wallet data is missing in response")
        if (nunchukNativeSdk.hasWallet(walletServer.localId.orEmpty()).not()) {
            val wallet = nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                name = walletServer.name.orEmpty()
                description = walletServer.description.orEmpty()
                createDate = walletServer.createdTimeMilis / 1000
            }
            nunchukNativeSdk.createWallet2(wallet)
        }

        return walletServer.toModel()
    }
}

