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

package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.type.SignerType
import javax.inject.Inject

class MasterSignerMapper @Inject constructor(
    accountManager: AccountManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
) {
    private val tapSignerCardIds = hashMapOf<String, String>()
    private val accountInfo = accountManager.getAccount()

    suspend operator fun invoke(from: MasterSigner, derivationPath: String = ""): SignerModel {
        val isPrimaryKey =
            accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == from.device.masterFingerprint
        if (from.type == SignerType.NFC && tapSignerCardIds.contains(from.id).not()) {
            tapSignerCardIds[from.id] =
                getTapSignerStatusByIdUseCase(from.id).getOrNull()?.ident.orEmpty()
        }
        return SignerModel(
            id = from.id,
            name = from.name,
            derivationPath = derivationPath.ifEmpty { from.device.path },
            fingerPrint = from.device.masterFingerprint,
            type = from.type,
            software = from.software,
            isPrimaryKey = isPrimaryKey,
            cardId = tapSignerCardIds[from.id].orEmpty()
        )
    }
}