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

package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateSoftwareSignerUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UseCase<CreateSoftwareSignerUseCase.Param, MasterSigner>(ioDispatcher) {

    override suspend fun execute(parameters: Param): MasterSigner {
        val masterSigner = nativeSdk.createSoftwareSigner(
            name = parameters.name,
            mnemonic = parameters.mnemonic,
            passphrase = parameters.passphrase,
            isPrimary = parameters.isPrimaryKey,
            replace = parameters.replace
        )
        if (parameters.isBackupNow.not()) {
            nativeSdk.updateMasterSigner(
                masterSigner.copy(isNeedBackup = true)
            )
        }
        return masterSigner
    }

    data class Param(
        val name: String,
        val mnemonic: String,
        val passphrase: String,
        val isPrimaryKey: Boolean,
        val replace: Boolean = false,
        val isBackupNow: Boolean,
    )
}