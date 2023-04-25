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

package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelableArrayList

data class InputAmountArgs(
    val roomId: String = "",
    val walletId: String,
    val availableAmount: Double,
    val inputs: List<UnspentOutput> = emptyList(),
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        InputAmountActivity::class.java
    ).apply {
        putExtra(EXTRA_ROOM_ID, roomId)
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putParcelableArrayListExtra(EXTRA_INPUT, ArrayList(inputs))
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_INPUT = "EXTRA_INPUT"

        fun deserializeFrom(intent: Intent) = InputAmountArgs(
            intent.extras.getStringValue(EXTRA_ROOM_ID),
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            intent.extras?.parcelableArrayList<UnspentOutput>(EXTRA_INPUT).orEmpty(),
        )
    }
}