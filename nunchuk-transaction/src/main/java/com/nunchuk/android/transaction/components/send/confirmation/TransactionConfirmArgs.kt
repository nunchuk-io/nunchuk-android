/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getIntValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SatsCardSlot

data class TransactionConfirmArgs(
    val walletId: String, // in case sweep it's target wallet id, other cases it's source wallet id
    val outputAmount: Double,
    val availableAmount: Double,
    val address: String,
    val privateNote: String,
    val estimatedFee: Double,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeRate: Int = 0,
    val sweepType: SweepType,
    val slots: List<SatsCardSlot>,
    val masterSignerId: String,
    val magicalPhrase: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, TransactionConfirmActivity::class.java).apply {
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
            putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
            putExtra(EXTRA_ADDRESS, address)
            putExtra(EXTRA_PRIVATE_NOTE, privateNote)
            putExtra(EXTRA_ESTIMATE_FEE, estimatedFee)
            putExtra(EXTRA_SUBTRACT_FEE_FROM_AMOUNT, subtractFeeFromAmount)
            putExtra(EXTRA_MANUAL_FEE_RATE, manualFeeRate)
            putExtra(EXTRA_SWEEP_TYPE, sweepType)
            putExtra(EXTRA_MASTER_SIGNER_ID, masterSignerId)
            putExtra(EXTRA_MASTER_SIGNER_ID, magicalPhrase)
            putParcelableArrayListExtra(EXTRA_SLOTS, ArrayList(slots))
        }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_ESTIMATE_FEE = "EXTRA_ESTIMATE_FEE"
        private const val EXTRA_SUBTRACT_FEE_FROM_AMOUNT = "EXTRA_SUBTRACT_FEE_FROM_AMOUNT"
        private const val EXTRA_MANUAL_FEE_RATE = "EXTRA_MANUAL_FEE_RATE"
        private const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"
        private const val EXTRA_SLOTS = "EXTRA_SLOTS"
        private const val EXTRA_MASTER_SIGNER_ID = "EXTRA_MASTER_SIGNER_ID"
        private const val EXTRA_MAGICAL_PHRASE = "EXTRA_MAGICAL_PHRASE"

        fun deserializeFrom(intent: Intent): TransactionConfirmArgs {
            val extras = intent.extras
            return TransactionConfirmArgs(
                extras.getStringValue(EXTRA_WALLET_ID),
                extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
                extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
                extras.getStringValue(EXTRA_ADDRESS),
                extras.getStringValue(EXTRA_PRIVATE_NOTE),
                extras.getDoubleValue(EXTRA_ESTIMATE_FEE),
                extras.getBooleanValue(EXTRA_SUBTRACT_FEE_FROM_AMOUNT),
                extras.getIntValue(EXTRA_MANUAL_FEE_RATE),
                extras!!.getSerializable(EXTRA_SWEEP_TYPE) as SweepType,
                extras.getParcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
                masterSignerId = extras.getStringValue(EXTRA_MASTER_SIGNER_ID),
                magicalPhrase = extras.getStringValue(EXTRA_MAGICAL_PHRASE)
            )
        }
    }
}