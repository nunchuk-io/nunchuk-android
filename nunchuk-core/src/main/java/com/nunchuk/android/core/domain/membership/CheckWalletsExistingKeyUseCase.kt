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

import android.os.Parcelable
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class CheckWalletsExistingKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val userWalletsRepository: PremiumWalletRepository,
    private val nunchukNativeSdk: NunchukNativeSdk
) : UseCase<Boolean, List<WalletsExistingKey>>(dispatcher) {
    override suspend fun execute(parameters: Boolean): List<WalletsExistingKey> {
        val wallets =
            if (parameters) userWalletsRepository.getUserWalletsServer().filter { it.status == "ACTIVE" } else userWalletsRepository.getGroupWalletsServer().filter { it.status == "ACTIVE" }
        val serverSigners: HashMap<String, SignerServer> = HashMap()
        wallets.forEach { wallet ->
            wallet.signers.forEach { signer ->
                signer.xfp?.let { xfp ->
                    serverSigners[xfp] = signer
                }
            }
        }
        val resultServerSigners = mutableListOf<WalletsExistingKey>()
        wallets.forEach { wallet ->
             val w = kotlin.runCatching {
                 nunchukNativeSdk.getWallet(wallet.localId)
             }.getOrNull() ?: return@forEach
            val localSigners = w.signers
            for (localSigner in localSigners) {
                if (!localSigner.isVisible || localSigner.type == SignerType.SERVER) {
                    continue
                }
                val serverSigner = serverSigners[localSigner.masterFingerprint]
                if (serverSigner != null && localSigner.type != serverSigner.type) {
                    if (localSigner.type == SignerType.SOFTWARE) {
                        resultServerSigners.add(WalletsExistingKey(serverSigner, localSigner))
                    } else {
                        val tapsigner = serverSigner.tapsigner
                        if (tapsigner != null) {
                            nunchukNativeSdk.addTapSigner(
                                cardId = tapsigner.cardId,
                                name = serverSigner.name.orEmpty(),
                                xfp = serverSigner.xfp.orEmpty(),
                                version = tapsigner.version.orEmpty(),
                                brithHeight = tapsigner.birthHeight,
                                isTestNet = tapsigner.isTestnet,
                                replace = true
                            )
                        } else {
                            nunchukNativeSdk.createSigner(
                                name = serverSigner.name.orEmpty(),
                                xpub = serverSigner.xpub.orEmpty(),
                                publicKey = serverSigner.pubkey.orEmpty(),
                                derivationPath = serverSigner.derivationPath.orEmpty(),
                                masterFingerprint = serverSigner.xfp.orEmpty(),
                                type = serverSigner.type,
                                tags = serverSigner.tags.mapNotNull { tag -> tag.toSignerTag() },
                                replace = true
                            )
                        }
                    }
                }
            }
        }
        return resultServerSigners
    }
}

@Parcelize
data class WalletsExistingKey(
    val signerServer: SignerServer,
    val localSigner: SingleSigner,
) : Parcelable