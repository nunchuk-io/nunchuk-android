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

package com.nunchuk.android.wallet

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType

sealed class ConfigureWalletEvent {
    object ChangeBip32Success : ConfigureWalletEvent()
    data class Loading(val loading: Boolean) : ConfigureWalletEvent()
    data class AssignSignerCompletedEvent(
        val totalRequireSigns: Int = 0,
        val signers: List<SingleSigner>,
    ) : ConfigureWalletEvent()
    data object OpenConfigKeySet : ConfigureWalletEvent()
    data class PromptInputPassphrase(val signer: SignerModel) : ConfigureWalletEvent()
    data class ShowError(val message: String) : ConfigureWalletEvent()
    data class ShowRiskSignerDialog(val isShow: Boolean) : ConfigureWalletEvent()
    data class RequestCacheTapSignerXpub(val signer: SignerModel) : ConfigureWalletEvent()
    data class CacheTapSignerXpubError(val error: Throwable?) : ConfigureWalletEvent()
    data class NfcLoading(val isLoading: Boolean) : ConfigureWalletEvent()
}

data class ConfigureWalletState(
    val totalRequireSigns: Int = 1,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val allSigners : List<SignerModel> = emptyList(),
    val selectedSigners: Set<SignerModel> = emptySet(),
    val isShowPath: Boolean = false,
    val supportedSignerTypes: Set<SignerType> = emptySet(),
    val keySet: Set<SignerModel> = emptySet(),
)