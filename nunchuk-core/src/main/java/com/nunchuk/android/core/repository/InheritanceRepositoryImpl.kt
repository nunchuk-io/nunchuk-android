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
import com.nunchuk.android.core.gateway.SignerGateway
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.ServerSignerMapper
import com.nunchuk.android.core.mapper.toInheritanceClaimingInit
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.InheritanceRepository
import com.nunchuk.android.type.SignerTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class InheritanceRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val serverSignerMapper: ServerSignerMapper,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val ncDataStore: NcDataStore,
    private val signerGateway: SignerGateway,
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
        val walletServer = response.data.wallet ?: throw NunchukApiException(code = 831) // inheritance not found
        val walletLocalId = walletServer.localId.orEmpty()
        if (nunchukNativeSdk.hasWallet(walletLocalId).not()) {
            val wallet = nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                name = walletServer.name.orEmpty()
                description = walletServer.description.orEmpty()
                createDate = System.currentTimeMillis() / 1000
            }
            nunchukNativeSdk.createWallet2(wallet)
        }

        walletServer.signerServerDtos?.forEach { signer ->
            signerGateway.saveServerSignerIfNeed(signer)
        }

        // Add wallet to claim wallets set
        val currentClaimWallets = ncDataStore.claimWalletsFlow.first().toMutableSet()
        currentClaimWallets.add(walletLocalId)
        ncDataStore.setClaimWallets(currentClaimWallets)

        return walletServer.toModel()
    }

    override suspend fun isClaimWallet(walletId: String): Boolean {
        val claimWallets = ncDataStore.claimWalletsFlow.first()
        return claimWallets.contains(walletId)
    }

    override fun getClaimWalletsFlow(): Flow<Set<String>> {
        return ncDataStore.claimWalletsFlow
    }
}

