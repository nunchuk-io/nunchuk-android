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

package com.nunchuk.android.transaction.components.details

import com.nunchuk.android.type.TransactionStatus

sealed class TransactionDetailsEvent {
    data class SignTransactionSuccess(
        val roomId: String = "",
        val serverSigned: Boolean? = null,
        val status: TransactionStatus? = null
    ) : TransactionDetailsEvent()

    data class BroadcastTransactionSuccess(val roomId: String = "") : TransactionDetailsEvent()

    data class DeleteTransactionSuccess(val isCancel: Boolean = false) : TransactionDetailsEvent()

    data class PromptInputPassphrase(val func: (String) -> Unit) : TransactionDetailsEvent()

    data class TransactionDetailsError(val message: String, val e: Throwable? = null) :
        TransactionDetailsEvent()

    object NoInternetConnection : TransactionDetailsEvent()

    data class ViewBlockchainExplorer(val url: String) : TransactionDetailsEvent()

    data class PromptTransactionOptions(
        val isPendingTransaction: Boolean,
        val isPendingConfirm: Boolean,
        val isRejected: Boolean,
        val canBroadcast: Boolean,
        val masterFingerPrint: String = ""
    ) : TransactionDetailsEvent()

    data class ExportToFileSuccess(val filePath: String) : TransactionDetailsEvent()

    data class TransactionError(val message: String) : TransactionDetailsEvent()

    data class UpdateTransactionMemoSuccess(val newMemo: String) : TransactionDetailsEvent()

    data class UpdateTransactionMemoFailed(val message: String) : TransactionDetailsEvent()

    object ImportTransactionFromMk4Success : TransactionDetailsEvent()

    object ExportTransactionToMk4Success : TransactionDetailsEvent()

    object LoadingEvent : TransactionDetailsEvent()
    object CancelScheduleBroadcastTransactionSuccess : TransactionDetailsEvent()

    data class NfcLoadingEvent(val isColdcard: Boolean = false) : TransactionDetailsEvent()
    object ImportTransactionSuccess : TransactionDetailsEvent()
    data class GetRawTransactionSuccess(val rawTransaction: String) : TransactionDetailsEvent()
}