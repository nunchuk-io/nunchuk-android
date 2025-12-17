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

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.InheritanceClaimingDownloadWalletRequest
import com.nunchuk.android.core.data.model.InheritanceClaimingInitRequest
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.exception.RequestAddKeyCancelException
import com.nunchuk.android.core.gateway.SignerGateway
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.ServerSignerMapper
import com.nunchuk.android.core.mapper.toInheritanceClaimingInit
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.RequestAddKeyDao
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.repository.InheritanceRepository
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class InheritanceRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val serverSignerMapper: ServerSignerMapper,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val ncDataStore: NcDataStore,
    private val signerGateway: SignerGateway,
    private val requestAddKeyDao: RequestAddKeyDao,
    private val accountManager: AccountManager,
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
        val walletServer =
            response.data.wallet ?: throw NunchukApiException(code = 831) // inheritance not found
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

        // Store user ID who successfully claimed wallet
        val chatId = accountManager.getAccount().chatId
        val chain = ncDataStore.chain.first()
        ncDataStore.setClaimedWalletUserId(chatId + chain.toString())

        return walletServer.toModel()
    }

    override suspend fun isClaimWallet(walletId: String): Boolean {
        val claimWallets = ncDataStore.claimWalletsFlow.first()
        return claimWallets.contains(walletId)
    }

    override fun getClaimWalletsFlow(): Flow<Set<String>> {
        return ncDataStore.claimWalletsFlow
    }

    override suspend fun getClaimingWallet(localId: String): WalletServer {
        val response = userWalletApiManager.claimInheritanceApi.getClaimingWallet(localId)
        val walletServer = response.data.wallet
            ?: throw IllegalStateException("Wallet data is missing in response")

        return walletServer.toModel()
    }

    override suspend fun requestAddKeyForInheritance(magic: String): String {
        val chatId = accountManager.getAccount().chatId
        val chain = ncDataStore.chain.first()

        // For inheritance, we use magic as groupId and leave tag empty
        // Use a default step since we don't have step parameter
        val defaultStep = MembershipStep.IRON_ADD_HARDWARE_KEY_1
        var localRequest = requestAddKeyDao.getRequest(
            chatId = chatId,
            chain = chain,
            step = MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            tag = "",
            groupId = magic
        )

        if (localRequest != null) {
            val response =
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            if (response.data.request == null) {
                requestAddKeyDao.delete(localRequest)
                localRequest = null
            }
        }

        return if (localRequest == null) {
            // For inheritance claiming, only pass magic
            val desktopKeyRequest = DesktopKeyRequest(
                tags = emptyList(),
                keyIndex = null,
                keyIndices = null,
                magic = magic
            )
            val response = userWalletApiManager.walletApi.requestAddKey(desktopKeyRequest)
            val requestId = response.data.request?.id.orEmpty()
            requestAddKeyDao.insert(
                RequestAddKeyEntity(
                    requestId = requestId,
                    chain = chain,
                    chatId = chatId,
                    step = defaultStep, // Use default step
                    tag = "", // Leave empty for inheritance
                    groupId = magic // Use magic as groupId
                )
            )
            requestId
        } else {
            userWalletApiManager.walletApi.pushRequestAddKey(localRequest.requestId)
            localRequest.requestId
        }
    }

    override suspend fun checkKeyAddedForInheritance(
        magic: String,
        requestId: String?
    ): Boolean {
        val chatId = accountManager.getAccount().chatId
        val chain = ncDataStore.chain.first()

        val localRequests = if (requestId == null) {
            requestAddKeyDao.getRequests(chatId, chain, magic)
        } else {
            requestAddKeyDao.getRequest(requestId)?.let { listOf(it) }.orEmpty()
        }

        localRequests.forEach { localRequest ->
            val response =
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            val request = response.data.request
            if (request?.status == "COMPLETED") {
                val isMiniscript = request.walletType == WalletType.MINISCRIPT.name
                val keysToProcess = if (isMiniscript) {
                    request.keys.orEmpty()
                } else {
                    request.key?.let { listOf(it) }.orEmpty()
                }

                if (keysToProcess.isEmpty()) {
                    // No keys to process, handle as before
                    if (request.key == null && request.keys.isNullOrEmpty()) {
                        requestAddKeyDao.delete(localRequest)
                        throw RequestAddKeyCancelException
                    }
                }

                keysToProcess.forEach { key ->
                    val type = nunchukNativeSdk.signerTypeFromStr(key.type.orEmpty())

                    val hasSigner = nunchukNativeSdk.hasSigner(
                        SingleSigner(
                            name = key.name.orEmpty(),
                            xpub = key.xpub.orEmpty(),
                            publicKey = key.pubkey.orEmpty(),
                            derivationPath = key.derivationPath.orEmpty(),
                            masterFingerprint = key.xfp.orEmpty(),
                        )
                    )
                    if (!hasSigner) {
                        nunchukNativeSdk.createSigner(
                            name = key.name.orEmpty(),
                            xpub = key.xpub.orEmpty(),
                            publicKey = key.pubkey.orEmpty(),
                            derivationPath = key.derivationPath.orEmpty(),
                            masterFingerprint = key.xfp.orEmpty(),
                            type = type,
                            tags = key.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() },
                            replace = false
                        )
                    }
                }

                if (requestId != null) return true
            } else if (request == null) {
                requestAddKeyDao.delete(localRequest)
                throw RequestAddKeyCancelException
            }
        }

        return false
    }

    override suspend fun deletePendingRequestsByMagic(magic: String) {
        requestAddKeyDao.deleteRequests(magic)
    }

    override suspend fun getAddedKeys(magic: String): Map<String, SingleSigner> {
        val result = mutableMapOf<String, SingleSigner>()
        requestAddKeyDao.getRequests(magic).map { localRequest ->
            val response =
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)

            response.data.request?.keys?.forEach { serverKey ->
                result[localRequest.requestId] = nunchukNativeSdk.getRemoteSigner(
                    masterFingerprint = serverKey.xfp.orEmpty(),
                    derivationPath = serverKey.derivationPath.orEmpty(),
                )
            }
        }
        return result
    }

    override fun isCurrentUserClaimedWallet(): Flow<Boolean> {
        val chatId = accountManager.getAccount().chatId
        return combine(
            ncDataStore.chain,
            ncDataStore.claimedWalletUserIdsFlow,
            ncDataStore.claimWalletsFlow
        ) { chain, claimedUserIds, claimWallets ->
            val userId = chatId + chain.toString()
            claimWallets.isNotEmpty() || claimedUserIds.contains(userId)
        }
    }
}

