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

package com.nunchuk.android.nav.args

import android.os.Bundle
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

data class AddReceiptArgs(
    val walletId: String,
    val txId: String? = null,
    val outputAmount: Double = 0.0,
    val availableAmount: Double = 0.0,
    val subtractFeeFromAmount: Boolean = false,
    val slots: List<SatsCardSlot> = emptyList(),
    val address: String = "",
    val privateNote: String = "",
    val sweepType: SweepType = SweepType.NONE,
    val inputs: List<UnspentOutput> = emptyList(),
    val claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
    val type: AddReceiptType = AddReceiptType.ADD_RECEIPT,
) {

    fun buildBundle(): Bundle = Bundle().apply {
        putString(EXTRA_WALLET_ID, walletId)
        putDouble(EXTRA_OUTPUT_AMOUNT, outputAmount)
        putDouble(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putBoolean(EXTRA_SUBTRACT_FEE, subtractFeeFromAmount)
        putSerializable(EXTRA_SWEEP_TYPE, sweepType)
        putString(EXTRA_ADDRESS, address)
        putString(EXTRA_PRIVATE_NOTE, privateNote)
        putParcelableArrayList(EXTRA_SLOTS, ArrayList(slots))
        putParcelable(EXTRA_CLAIM_INHERITANCE_TX_PARAM, claimInheritanceTxParam)
        putParcelableArrayList(EXTRA_INPUT, ArrayList(inputs))
        putSerializable(EXTRA_RECEIPT_TYPE, type)
        putBoolean(EXTRA_IS_FROM_SELECTED_COIN, inputs.isNotEmpty())
        txId?.let { putString(EXTRA_TX_ID, it) }
    }

    companion object {
        const val EXTRA_WALLET_ID = "wallet_id"
        const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        const val EXTRA_AVAILABLE_AMOUNT = "available_amount"
        const val EXTRA_SUBTRACT_FEE = "EXTRA_SUBTRACT_FEE"
        const val EXTRA_SLOTS = "EXTRA_SLOTS"
        const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"
        const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        const val EXTRA_CLAIM_INHERITANCE_TX_PARAM = "EXTRA_CLAIM_INHERITANCE_TX_PARAM"
        const val EXTRA_INPUT = "unspent_outputs"
        const val EXTRA_RECEIPT_TYPE = "EXTRA_RECEIPT_TYPE"
        const val EXTRA_IS_FROM_SELECTED_COIN = "is_from_select_coin"
        const val EXTRA_TX_ID = "EXTRA_TX_ID"

        fun fromBundle(bundle: Bundle): AddReceiptArgs = AddReceiptArgs(
            walletId = bundle.getString(EXTRA_WALLET_ID) ?: "",
            outputAmount = bundle.getDouble(EXTRA_OUTPUT_AMOUNT),
            availableAmount = bundle.getDouble(EXTRA_AVAILABLE_AMOUNT),
            subtractFeeFromAmount = bundle.getBoolean(EXTRA_SUBTRACT_FEE),
            slots = bundle.parcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
            address = bundle.getString(EXTRA_ADDRESS) ?: "",
            privateNote = bundle.getString(EXTRA_PRIVATE_NOTE) ?: "",
            sweepType = bundle.serializable<SweepType>(EXTRA_SWEEP_TYPE)!!,
            inputs = bundle.parcelableArrayList<UnspentOutput>(EXTRA_INPUT) ?: emptyList(),
            claimInheritanceTxParam = bundle.parcelable(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
            type = bundle.serializable<AddReceiptType>(EXTRA_RECEIPT_TYPE) ?: AddReceiptType.ADD_RECEIPT,
            txId = bundle.getString(EXTRA_TX_ID, null),
        )
    }
}
