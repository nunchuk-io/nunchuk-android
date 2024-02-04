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

package com.nunchuk.android.transaction.components.details.fee

import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.Transaction

data class ReplaceFeeState(
    val fee: EstimateFeeRates = EstimateFeeRates(),
    val manualFeeRate: Int = fee.standardRate,
    val previousFeeRate: Int = 0,
)

sealed class ReplaceFeeEvent {
    data class Loading(val isLoading: Boolean) : ReplaceFeeEvent()
    data class ReplaceTransactionSuccess(val newTxId: String) : ReplaceFeeEvent()
    data class DraftTransactionSuccess(val transaction: Transaction, val newFee: Int) :
        ReplaceFeeEvent()

    data class ShowError(val e: Throwable?) : ReplaceFeeEvent()
}