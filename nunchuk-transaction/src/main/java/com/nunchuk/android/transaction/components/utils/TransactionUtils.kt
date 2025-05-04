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

package com.nunchuk.android.transaction.components.utils

import android.content.Context
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.transaction.R

fun SweepType.toTitle(context: Context, label: String, isConfirmTx: Boolean = false) = when (this) {
    SweepType.NONE -> label
    SweepType.SWEEP_TO_NUNCHUK_WALLET,
    SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET -> context.getString(R.string.nc_sweep_to_a_wallet)
    SweepType.SWEEP_TO_EXTERNAL_ADDRESS,
    SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS -> if (isConfirmTx) context.getString(R.string.nc_transaction_confirm_transaction) else context.getString(R.string.nc_withdraw_to_an_address)
}

val BtcUri.privateNote: String
    get() = message.ifEmpty { label }