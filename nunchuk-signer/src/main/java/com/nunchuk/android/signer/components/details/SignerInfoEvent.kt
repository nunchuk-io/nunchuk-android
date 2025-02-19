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

package com.nunchuk.android.signer.components.details

import com.nunchuk.android.model.HealthCheckHistory
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.byzantine.DummyTransactionPayload

sealed class SignerInfoEvent {

    data object NfcLoading : SignerInfoEvent()

    data class UpdateNameSuccessEvent(val signerName: String) : SignerInfoEvent()

    data class UpdateNameErrorEvent(val message: String) : SignerInfoEvent()

    data object RemoveSignerCompletedEvent : SignerInfoEvent()

    data class RemoveSignerErrorEvent(val message: String) : SignerInfoEvent()

    data object HealthCheckSuccessEvent : SignerInfoEvent()

    data class HealthCheckErrorEvent(val message: String? = null, val e: Throwable? = null) :
        SignerInfoEvent()

    data class GetTapSignerBackupKeyEvent(val backupKeyPath: String) : SignerInfoEvent()

    data class NfcError(val e: Throwable?) : SignerInfoEvent()

    data object GenerateColdcardHealthMessagesSuccess : SignerInfoEvent()

    data object TopUpXpubSuccess : SignerInfoEvent()

    data class TopUpXpubFailed(val e: Throwable?) : SignerInfoEvent()
    data class Error(val e: Throwable) : SignerInfoEvent()
    data class DeleteExistingSignerSuccess(val keyName: String) : SignerInfoEvent()
    data class Loading(val loading: Boolean) : SignerInfoEvent()
    class GetHealthCheckPayload(val payload: DummyTransactionPayload, val walletId: String, val groupId: String) : SignerInfoEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : SignerInfoEvent()
}

data class SignerInfoState(
    val remoteSigner: SingleSigner? = null,
    val masterSigner: MasterSigner? = null,
    val nfcCardId: String? = null,
    val healthCheckHistories: List<HealthCheckHistory>? = null,
    val lastHealthCheckTimeMillis: Long = 0,
    val signerName: String = "",
    val assistedWalletIds: List<String> = emptyList(),
    val backupKeyPath: String = "",
)