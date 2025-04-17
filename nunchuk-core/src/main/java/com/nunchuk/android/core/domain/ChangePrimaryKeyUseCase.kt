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

package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ChangePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val signerSoftwareRepository: SignerSoftwareRepository,
    private val accountManager: AccountManager,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
    private val reloadPrimaryKeyInfoUseCase: ReloadPrimaryKeyInfoUseCase
) : UseCase<ChangePrimaryKeyUseCase.Param, MasterSigner?>(dispatcher) {
    override suspend fun execute(parameters: Param): MasterSigner? {
        val oldSignerInfo = primaryKeySignerInfoHolder.getSignerInfo() ?: return null

        if (primaryKeySignerInfoHolder.isNeedPassphraseSent()) {
            nunchukNativeSdk.sendSignerPassphrase(
                oldSignerInfo.id,
                parameters.oldKeyPassphrase
            )
        }

        val newAddress =
            nunchukNativeSdk.getPrimaryKeyAddress(parameters.mnemonic, parameters.newKeyPassphrase)
        if (newAddress.isNullOrEmpty()) return null

        val newKeyMasterFingerprint =
            nunchukNativeSdk.getMasterFingerprint(parameters.mnemonic, parameters.newKeyPassphrase) ?: return null

        val nonce = signerSoftwareRepository.postPKeyNonce(
            address = null,
            username = accountManager.getAccount().name,
            nonce = newAddress,
            isChangeKey = true
        )
        val oldSignature = nunchukNativeSdk.signLoginMessageImpl(
            oldSignerInfo.device.masterFingerprint,
            nonce
        ) ?: return null
        val newSignature =
            nunchukNativeSdk.signLoginMessage(
                parameters.mnemonic,
                parameters.newKeyPassphrase,
                nonce
            ) ?: return null
        signerSoftwareRepository.pKeyChangeKey(newAddress, oldSignature, newSignature)

        val newSigner = nunchukNativeSdk.createSoftwareSigner(
            name = parameters.signerName,
            mnemonic = parameters.mnemonic,
            passphrase = parameters.newKeyPassphrase,
            isPrimary = true,
            replace = false,
            primaryDecoyPin = accountManager.getAccount().decoyPin,
        )

        val primaryKeyInfo = accountManager.getPrimaryKeyInfo()
        primaryKeyInfo?.let {
            accountManager.storePrimaryKeyInfo(
                primaryKeyInfo.copy(xfp = newKeyMasterFingerprint)
            )
        }

        reloadPrimaryKeyInfoUseCase.invoke(
            ReloadPrimaryKeyInfoUseCase.Param(
                ReloadPrimaryKeyInfoUseCase.InfoType.ALL
            )
        )

        return newSigner
    }

    class Param(
        val mnemonic: String,
        val newKeyPassphrase: String,
        val signerName: String,
        val oldKeyPassphrase: String
    )
}