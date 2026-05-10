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

enum class StablecoinToken { USDT, LBTC }

sealed class InputStablecoinAmountEvent {
    data class AcceptAmountEvent(val amount: Double, val token: StablecoinToken) : InputStablecoinAmountEvent()
    object InvalidAmountEvent : InputStablecoinAmountEvent()
    object InsufficientFundsEvent : InputStablecoinAmountEvent()
    data class ShowError(val message: String) : InputStablecoinAmountEvent()
}

data class InputStablecoinAmountState(
    val selectedToken: StablecoinToken = StablecoinToken.USDT,
    val useToken: Boolean = true,
    val inputText: String = "",
    val amountToken: Double = 0.0,
    val amountUsd: Double = 0.0,
    // TODO(stablecoin): wire to real balance source.
    val usdtBalance: Double = 1024.0,
    val usdtBalanceUsd: Double = 1024.25,
    val lbtcBalance: Double = 0.0004,
    val lbtcBalanceUsd: Double = 29.80,
    // TODO(stablecoin): wire to real fee estimator.
    val networkFeeLbtc: Double = 0.0,
    val networkFeeUsd: Double = 0.0,
)
