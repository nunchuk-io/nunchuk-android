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

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getIntValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

data class TransactionConfirmArgs(
    val walletId: String, // in case sweep it's target wallet id, other cases it's source wallet id
    val availableAmount: Double,
    val txReceipts: List<TxReceipt>,
    val privateNote: String,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeRate: Int = 0,
    val sweepType: SweepType,
    val slots: List<SatsCardSlot>,
    val claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
    val inputs: List<UnspentOutput> = emptyList(),
    val actionButtonText: String = ""
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, TransactionConfirmActivity::class.java).apply {
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
            putExtra(EXTRA_PRIVATE_NOTE, privateNote)
            putExtra(EXTRA_SUBTRACT_FEE_FROM_AMOUNT, subtractFeeFromAmount)
            putExtra(EXTRA_MANUAL_FEE_RATE, manualFeeRate)
            putExtra(EXTRA_SWEEP_TYPE, sweepType)
            putExtra(EXTRA_CLAIM_INHERITANCE_TX_PARAM, claimInheritanceTxParam)
            putParcelableArrayListExtra(EXTRA_TX_RECEIPTS, ArrayList(txReceipts))
            putParcelableArrayListExtra(EXTRA_SLOTS, ArrayList(slots))
            putParcelableArrayListExtra(EXTRA_INPUTS, ArrayList(inputs))
            putExtra(EXTRA_ACTION_BUTTON_TEXT, actionButtonText)
        }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_SUBTRACT_FEE_FROM_AMOUNT = "EXTRA_SUBTRACT_FEE_FROM_AMOUNT"
        private const val EXTRA_MANUAL_FEE_RATE = "EXTRA_MANUAL_FEE_RATE"
        private const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"
        private const val EXTRA_SLOTS = "EXTRA_SLOTS"
        private const val EXTRA_INPUTS = "EXTRA_INPUTS"
        private const val EXTRA_TX_RECEIPTS = "EXTRA_TX_RECEIPTS"
        private const val EXTRA_CLAIM_INHERITANCE_TX_PARAM = "EXTRA_CLAIM_INHERITANCE_TX_PARAM"
        private const val EXTRA_ACTION_BUTTON_TEXT = "EXTRA_ACTION_BUTTON_TEXT"

        fun deserializeFrom(intent: Intent): TransactionConfirmArgs {
            val extras = intent.extras
            return TransactionConfirmArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                availableAmount = extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
                privateNote = extras.getStringValue(EXTRA_PRIVATE_NOTE),
                subtractFeeFromAmount = extras.getBooleanValue(EXTRA_SUBTRACT_FEE_FROM_AMOUNT),
                manualFeeRate = extras.getIntValue(EXTRA_MANUAL_FEE_RATE),
                sweepType = extras?.serializable(EXTRA_SWEEP_TYPE)!!,
                slots = extras.parcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
                claimInheritanceTxParam = extras.parcelable(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
                inputs = extras.parcelableArrayList<UnspentOutput>(EXTRA_INPUTS).orEmpty(),
                txReceipts = extras.parcelableArrayList<TxReceipt>(EXTRA_TX_RECEIPTS).orEmpty(),
                actionButtonText = extras.getStringValue(EXTRA_ACTION_BUTTON_TEXT)
            )
        }
    }
}