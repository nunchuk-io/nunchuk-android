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

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.nfc.NfcViewModel.Companion.EXTRA_MASTER_SIGNER_ID
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

data class AddReceiptArgs(
    val walletId: String,
    val outputAmount: Double,
    val availableAmount: Double,
    val subtractFeeFromAmount: Boolean = false,
    val slots: List<SatsCardSlot>,
    val address: String = "",
    val privateNote: String = "",
    val sweepType: SweepType,
    val inputs: List<UnspentOutput>,
    val claimInheritanceTxParam: ClaimInheritanceTxParam? = null
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, AddReceiptActivity::class.java).apply {
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
            putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
            putExtra(EXTRA_SUBTRACT_FEE, subtractFeeFromAmount)
            putExtra(EXTRA_SWEEP_TYPE, sweepType)
            putExtra(EXTRA_ADDRESS, address)
            putExtra(EXTRA_PRIVATE_NOTE, privateNote)
            putParcelableArrayListExtra(EXTRA_SLOTS, ArrayList(slots))
            putExtra(EXTRA_CLAIM_INHERITANCE_TX_PARAM, claimInheritanceTxParam)
            putParcelableArrayListExtra(EXTRA_INPUT, ArrayList(inputs))
        }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_SUBTRACT_FEE = "EXTRA_SUBTRACT_FEE"
        private const val EXTRA_SLOTS = "EXTRA_SLOTS"
        private const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_CLAIM_INHERITANCE_TX_PARAM = "EXTRA_CLAIM_INHERITANCE_TX_PARAM"
        private const val EXTRA_INPUT = "EXTRA_INPUT"

        fun deserializeFrom(intent: Intent) = AddReceiptArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            intent.extras.getBooleanValue(EXTRA_SUBTRACT_FEE),
            intent.extras?.parcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
            intent.extras.getStringValue(EXTRA_ADDRESS),
            intent.extras.getStringValue(EXTRA_PRIVATE_NOTE),
            intent.extras?.serializable(EXTRA_SWEEP_TYPE)!!,
            intent.extras?.parcelableArrayList<UnspentOutput>(EXTRA_INPUT).orEmpty(),
            intent.extras?.parcelable<ClaimInheritanceTxParam>(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
        )
    }
}