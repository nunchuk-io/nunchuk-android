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

package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList

data class InputAmountArgs(
    val walletId: String,
    val availableAmount: Double,
    val inputs: List<UnspentOutput> = emptyList(),
    val claimInheritanceTxParam: ClaimInheritanceTxParam?,
    val btcUri: BtcUri?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        InputAmountActivity::class.java
    ).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putParcelableArrayListExtra(EXTRA_INPUT, ArrayList(inputs))
        putExtra(EXTRA_CLAIM_INHERITANCE_TX_PARAM, claimInheritanceTxParam)
        putExtra(EXTRA_BTC_URI, btcUri)
    }

    companion object {
        const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        const val EXTRA_INPUT = "EXTRA_INPUT"
        const val EXTRA_CLAIM_INHERITANCE_TX_PARAM = "EXTRA_CLAIM_INHERITANCE_TX_PARAM"
        const val EXTRA_BTC_URI = "EXTRA_BTC_URI"

        fun deserializeFrom(intent: Intent) = InputAmountArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            intent.extras?.parcelableArrayList<UnspentOutput>(EXTRA_INPUT).orEmpty(),
            intent.extras?.parcelable<ClaimInheritanceTxParam>(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
            intent.extras?.parcelable<BtcUri>(EXTRA_BTC_URI)
        )

        fun fromSavedStateHandle(handle: SavedStateHandle) = InputAmountArgs(
            walletId = handle.get<String>(EXTRA_WALLET_ID).orEmpty(),
            availableAmount = handle.get<Double>(EXTRA_AVAILABLE_AMOUNT) ?: 0.0,
            inputs = handle.get<ArrayList<UnspentOutput>>(EXTRA_INPUT).orEmpty(),
            claimInheritanceTxParam = handle.get<ClaimInheritanceTxParam>(EXTRA_CLAIM_INHERITANCE_TX_PARAM),
            btcUri = handle.get<BtcUri>(EXTRA_BTC_URI),
        )
    }
}