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

package com.nunchuk.android.transaction.components.send.batchtransaction

sealed class BatchTransactionEvent {
    data class Loading(val loading: Boolean) : BatchTransactionEvent()
    data class Error(val message: String) : BatchTransactionEvent()
    data class CheckAddressSuccess(val isCustomTx: Boolean, val subtractFeeFromAmount: Boolean) : BatchTransactionEvent()
    data object InsufficientFundsEvent : BatchTransactionEvent()
    data object InsufficientFundsLockedCoinEvent : BatchTransactionEvent()
}

data class BatchTransactionState(
    val note: String = "",
    val recipients: List<Recipient> = initRecipientList(),
    val interactingIndex: Int = -1,
) {
    data class Recipient(
        val amount: String, val address: String, val isBtc: Boolean,
        val selectAddressType: Int, val selectAddressName: String,
        val invalidAddress: Boolean, val walletId: String
    ) {
        companion object {
            val DEFAULT = Recipient(
                amount = "",
                address = "",
                isBtc = true,
                selectAddressType = SelectAddressType.NONE.ordinal,
                selectAddressName = "",
                invalidAddress = false,
                walletId = ""
            )
        }
    }
}

enum class SelectAddressType {
    NONE, ADDRESS, WALLET
}

private fun initRecipientList(): List<BatchTransactionState.Recipient> {
    val recipients = arrayListOf<BatchTransactionState.Recipient>()
    recipients.add(BatchTransactionState.Recipient.DEFAULT)
    return recipients
}

