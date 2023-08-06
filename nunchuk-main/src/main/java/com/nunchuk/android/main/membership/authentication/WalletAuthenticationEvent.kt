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

package com.nunchuk.android.main.membership.authentication

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction

sealed class WalletAuthenticationEvent {
    data class Loading(val isLoading: Boolean) : WalletAuthenticationEvent()
    data class ProcessFailure(val message: String) : WalletAuthenticationEvent()
    data class ShowError(val message: String) : WalletAuthenticationEvent()
    data class WalletAuthenticationSuccess(val signatures: Map<String, String> = emptyMap()) :
        WalletAuthenticationEvent()

    class NfcLoading(val isLoading: Boolean, val isColdCard: Boolean = false) :
        WalletAuthenticationEvent()

    object ScanTapSigner : WalletAuthenticationEvent()
    object ScanColdCard : WalletAuthenticationEvent()
    object CanNotSignHardwareKey : WalletAuthenticationEvent()
    object GenerateColdcardHealthMessagesSuccess : WalletAuthenticationEvent()
    object ShowAirgapOption : WalletAuthenticationEvent()
    object ExportTransactionToColdcardSuccess : WalletAuthenticationEvent()
    object CanNotSignDummyTx : WalletAuthenticationEvent()
}

data class WalletAuthenticationState(
    val walletSigner: List<SignerModel> = emptyList(),
    val singleSigners: List<SingleSigner> = emptyList(),
    val signatures: Map<String, String> = emptyMap(),
    val transaction: Transaction? = null,
    val pendingSignature : Int = 0,
    val interactSingleSigner: SingleSigner? = null
)