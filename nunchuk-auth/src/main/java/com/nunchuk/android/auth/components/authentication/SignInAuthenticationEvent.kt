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

package com.nunchuk.android.auth.components.authentication

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.type.TransactionStatus

sealed class SignInAuthenticationEvent {
    data class Loading(val isLoading: Boolean) : SignInAuthenticationEvent()
    data class ProcessFailure(val message: String) : SignInAuthenticationEvent()
    data class ShowError(val message: String) : SignInAuthenticationEvent()
    class NfcLoading(val isLoading: Boolean, val isColdCard: Boolean = false) : SignInAuthenticationEvent()
    data object ScanTapSigner : SignInAuthenticationEvent()
    data object ScanColdCard : SignInAuthenticationEvent()
    data object CanNotSignHardwareKey : SignInAuthenticationEvent()
    data object ShowAirgapOption : SignInAuthenticationEvent()
    data object ExportTransactionToColdcardSuccess : SignInAuthenticationEvent()
    data object CanNotSignDummyTx : SignInAuthenticationEvent()
    data class SignFailed(val signerModel: SignerModel) : SignInAuthenticationEvent()
    data class SignInSuccess(val token: String, val deviceId: String): SignInAuthenticationEvent()
}

data class SignInAuthenticationState(
    val walletSigner: List<SignerModel> = emptyList(),
    val signatures: Map<String, String> = emptyMap(),
    val transaction: Transaction? = null,
    val pendingSignature: Int = 0,
    val interactSignerModel: SignerModel? = null,
    val dummyTransactionType: DummyTransactionType = DummyTransactionType.NONE,
    val enabledSigners: Set<String> = emptySet(),
    val transactionStatus: TransactionStatus = TransactionStatus.PENDING_SIGNATURES,
)