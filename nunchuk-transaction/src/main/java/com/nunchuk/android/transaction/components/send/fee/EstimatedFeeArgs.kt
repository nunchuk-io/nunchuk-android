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

package com.nunchuk.android.transaction.components.send.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

data class EstimatedFeeArgs(
    val walletId: String,
    val availableAmount: Double,
    val txReceipts: List<TxReceipt>,
    val privateNote: String,
    val subtractFeeFromAmount: Boolean = false,
    val sweepType: SweepType = SweepType.NONE,
    val slots: List<SatsCardSlot> = emptyList(),
    val claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
    val inputs: List<UnspentOutput> = emptyList(),
    val isConsolidateFlow: Boolean = false,
    val title: String = "",
    val rollOverWalletParam: RollOverWalletParam? = null,
    val confirmTxActionButtonText: String = "",
    val signingPath: SigningPath? = null,
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, EstimatedFeeActivity::class.java).apply {
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
            putExtra(EXTRA_PRIVATE_NOTE, privateNote)
            putExtra(EXTRA_SUBTRACT_FEE, subtractFeeFromAmount)
            putExtra(EXTRA_SWEEP_TYPE, sweepType)
            putParcelableArrayListExtra(EXTRA_SLOTS, ArrayList(slots))
            putParcelableArrayListExtra(EXTRA_INPUT, ArrayList(inputs))
            putParcelableArrayListExtra(EXTRA_TX_RECEIPTS, ArrayList(txReceipts))
            putExtra(EXTRA_CLAIM_INHERITANCE_TX_PARAM, claimInheritanceTxParam)
            putExtra(EXTRA_IS_CONSOLIDATE_FLOW, isConsolidateFlow)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ROLLOVER_WALLET, rollOverWalletParam)
            putExtra(EXTRA_CONFIRM_TX_ACTION_BUTTON_TEXT, confirmTxActionButtonText)
            putExtra(EXTRA_SIGNING_PATH, signingPath)
        }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_SUBTRACT_FEE = "EXTRA_SUBTRACT_FEE"
        private const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"
        private const val EXTRA_SLOTS = "EXTRA_SLOTS"
        private const val EXTRA_INPUT = "EXTRA_INPUT"
        private const val EXTRA_TX_RECEIPTS = "EXTRA_TX_RECEIPTS"
        private const val EXTRA_CLAIM_INHERITANCE_TX_PARAM = "EXTRA_CLAIM_INHERITANCE_TX_PARAM"
        private const val EXTRA_IS_CONSOLIDATE_FLOW = "EXTRA_IS_CONSOLIDATE_FLOW"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_ROLLOVER_WALLET = "EXTRA_ROLLOVER_WALLET"
        private const val EXTRA_CONFIRM_TX_ACTION_BUTTON_TEXT =
            "EXTRA_CONFIRM_TX_ACTION_BUTTON_TEXT"
        private const val EXTRA_SIGNING_PATH = "EXTRA_SIGNING_PATH"

        fun deserializeFrom(intent: Intent) = EstimatedFeeArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID),
            availableAmount = intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            privateNote = intent.extras.getStringValue(EXTRA_PRIVATE_NOTE),
            subtractFeeFromAmount = intent.extras.getBooleanValue(EXTRA_SUBTRACT_FEE),
            sweepType = intent.extras?.serializable(EXTRA_SWEEP_TYPE)!!,
            slots = intent.extras?.parcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
            claimInheritanceTxParam = intent.extras?.parcelable(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
            inputs = intent.extras?.parcelableArrayList<UnspentOutput>(EXTRA_INPUT).orEmpty(),
            txReceipts = intent.extras?.parcelableArrayList<TxReceipt>(EXTRA_TX_RECEIPTS).orEmpty(),
            isConsolidateFlow = intent.extras.getBooleanValue(EXTRA_IS_CONSOLIDATE_FLOW),
            title = intent.extras.getStringValue(EXTRA_TITLE),
            rollOverWalletParam = intent.extras?.parcelable(EXTRA_ROLLOVER_WALLET),
            confirmTxActionButtonText = intent.extras.getStringValue(
                EXTRA_CONFIRM_TX_ACTION_BUTTON_TEXT
            ),
            signingPath = intent.extras?.parcelable(EXTRA_SIGNING_PATH)
        )
    }
}