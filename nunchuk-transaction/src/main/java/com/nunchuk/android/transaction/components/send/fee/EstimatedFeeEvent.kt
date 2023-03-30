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

package com.nunchuk.android.transaction.components.send.fee

import com.nunchuk.android.model.*

sealed class EstimatedFeeEvent {
    class Loading(val isLoading: Boolean) : EstimatedFeeEvent()
    object InvalidManualFee : EstimatedFeeEvent()
    data class GetFeeRateSuccess(val estimateFeeRates: EstimateFeeRates) : EstimatedFeeEvent()
    data class EstimatedFeeErrorEvent(val message: String) : EstimatedFeeEvent()
    data class EstimatedFeeCompletedEvent(
        val estimatedFee: Double,
        val subtractFeeFromAmount: Boolean,
        val manualFeeRate: Int
    ) : EstimatedFeeEvent()
}

data class EstimatedFeeState(
    val estimatedFee: Amount = Amount.ZER0,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeDetails: Boolean = false,
    val estimateFeeRates: EstimateFeeRates = EstimateFeeRates(),
    val allTags: Map<Int, CoinTag> = emptyMap(),
    val allCoins: List<UnspentOutput> = emptyList(),
    val inputs: List<TxInput> = emptyList(),
    val manualFeeRate: Int = estimateFeeRates.defaultRate
)