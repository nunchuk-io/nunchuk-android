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

package com.nunchuk.android.transaction.components.send.confirmation

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput

sealed class TransactionConfirmEvent {
    data class LoadingEvent(val isClaimInheritance: Boolean = false) : TransactionConfirmEvent()
    data class CreateTxSuccessEvent(val transaction: Transaction) : TransactionConfirmEvent()
    data class AssignTagEvent(
        val walletId: String,
        val txId: String,
        val output: UnspentOutput,
        val tags: List<CoinTag>,
    ) : TransactionConfirmEvent()

    data class CreateTxErrorEvent(val message: String) : TransactionConfirmEvent()
    data class UpdateChangeAddress(val address: String, val amount: Amount) :
        TransactionConfirmEvent()

    data class DraftTransactionSuccess(val transaction: Transaction) : TransactionConfirmEvent()

    data class InitRoomTransactionError(val message: String) : TransactionConfirmEvent()
    data class InitRoomTransactionSuccess(val roomId: String) : TransactionConfirmEvent()
    data class AssignTagSuccess(val txId: String) : TransactionConfirmEvent()
    data class AssignTagError(val message: String) : TransactionConfirmEvent()
    data class DraftTaprootTransactionSuccess(val draftTransaction: TaprootDraftTransaction?) :
        TransactionConfirmEvent()
}