/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class DeletePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val signerSoftwareRepository: SignerSoftwareRepository,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
) : UseCase<DeletePrimaryKeyUseCase.Param, Unit>(dispatcher) {

    override suspend fun execute(parameters: Param) {
        val signerInfo = primaryKeySignerInfoHolder.getSignerInfo() ?: return
        val primaryKeyInfo = primaryKeySignerInfoHolder.getPrimaryKeyInfo() ?: return
        val nonce = signerSoftwareRepository.getPKeyNonce(address = primaryKeyInfo.address, username = primaryKeyInfo.account)
        val message = "${primaryKeyInfo.account}${nonce}"
        nunchukNativeSdk.clearSignerPassphrase(primaryKeyInfo.masterFingerprint)
        if (primaryKeySignerInfoHolder.isNeedPassphraseSent()) {
            nunchukNativeSdk.sendSignerPassphrase(
                signerInfo.id,
                parameters.passphrase
            )
        }
        val signature = nunchukNativeSdk.signLoginMessageImpl(primaryKeyInfo.masterFingerprint, message) ?: return
        nunchukNativeSdk.deletePrimaryKey()
        signerSoftwareRepository.pKeyDeleteAccount(signature)
    }

    class Param(val passphrase: String)
}