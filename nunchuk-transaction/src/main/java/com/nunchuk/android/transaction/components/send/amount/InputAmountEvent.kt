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

import com.nunchuk.android.model.BtcUri

sealed class InputAmountEvent {
    data class Loading(val isLoading: Boolean) : InputAmountEvent()
    data class AcceptAmountEvent(val amount: Double) : InputAmountEvent()
    data class SwapCurrencyEvent(val amount: Double) : InputAmountEvent()
    data class ParseBtcUriSuccess(val btcUri: BtcUri) : InputAmountEvent()
    data class ShowError(val message: String) : InputAmountEvent()
    object InsufficientFundsEvent : InputAmountEvent()
}

data class InputAmountState(
    val amountBTC: Double = 0.0,
    val amountUSD: Double = 0.0,
    val useBtc: Boolean = true,
    val address: String = "",
    val privateNote: String = "",
)