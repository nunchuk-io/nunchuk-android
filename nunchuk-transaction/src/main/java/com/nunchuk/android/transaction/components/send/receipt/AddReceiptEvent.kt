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

package com.nunchuk.android.transaction.components.send.receipt

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType

sealed class AddReceiptEvent {
    data object InvalidAddressEvent : AddReceiptEvent()
    data object AddressRequiredEvent : AddReceiptEvent()
    data class ShowError(val message: String) : AddReceiptEvent()
    data class AcceptedAddressEvent(
        val isCreateTransaction: Boolean,
        val isMiniscript: Boolean,
    ) : AddReceiptEvent()
    data object ParseBtcUriEvent : AddReceiptEvent()
    data class Loading(val isLoading: Boolean) : AddReceiptEvent()
    data object NoOp : AddReceiptEvent()
}

data class AddReceiptState(
    val address: String = "",
    val privateNote: String = "",
    val amount: Amount = Amount(),
    val addressType: AddressType = AddressType.ANY,
    val isValueKeySetDisable: Boolean = false,
    val signers : Map<String, SignerModel> = emptyMap(),
    val antiFeeSniping: Boolean = false,
    val scriptNode: ScriptNode? = null,
    val subNodeFollowParents: Set<List<Int>> = emptySet(),
    val wallet: Wallet = Wallet(),
)
