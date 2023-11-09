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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.CalculateRequiredSignaturesExt
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.membership.AssistedWalletBrief

sealed class KeyRecoveryIntroEvent {
    data class Loading(val isLoading: Boolean) : KeyRecoveryIntroEvent()
    data class GetTapSignerSuccess(val signers: List<SignerModel>) : KeyRecoveryIntroEvent()
    data class Error(val message: String) : KeyRecoveryIntroEvent()
    data class DownloadBackupKeySuccess(val backupKey: BackupKey) : KeyRecoveryIntroEvent()
    data class CalculateRequiredSignaturesSuccess(val calculateRequiredSignaturesExt: CalculateRequiredSignaturesExt) : KeyRecoveryIntroEvent()
    data object RequestRecoverSuccess : KeyRecoveryIntroEvent()
}

data class KeyRecoveryIntroState(
    val tapSigners: List<SignerModel> = emptyList(),
    val selectedSigner: SignerModel? = null,
    val assistedWallets: HashSet<String> = HashSet(),
    val wallets: List<WalletExtended> = emptyList(),
)