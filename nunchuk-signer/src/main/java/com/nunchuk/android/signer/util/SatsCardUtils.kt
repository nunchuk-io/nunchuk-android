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

package com.nunchuk.android.signer.util

import androidx.fragment.app.Fragment
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.nav.NunchukNavigator

internal fun Fragment.openSweepRecipeScreen(navigator: NunchukNavigator, slots: List<SatsCardSlot>, isSweepActiveSlot: Boolean) {
    val sweepType = if (isSweepActiveSlot) SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS else SweepType.SWEEP_TO_EXTERNAL_ADDRESS
    val totalBalance = slots.sumOf { it.balance.value }
    val totalInBtc = Amount(value = totalBalance).pureBTC()
    navigator.openAddReceiptScreen(
        activityContext = requireActivity(),
        walletId = "",
        outputAmount = totalInBtc,
        availableAmount = totalInBtc,
        subtractFeeFromAmount = true,
        slots = slots,
        sweepType = sweepType
    )
}