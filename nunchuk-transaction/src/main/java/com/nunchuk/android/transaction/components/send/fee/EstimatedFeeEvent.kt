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

package com.nunchuk.android.transaction.components.send.fee

import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.PairAmount
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate

sealed class EstimatedFeeEvent {
    class Loading(val isLoading: Boolean) : EstimatedFeeEvent()
    data object InvalidManualFee : EstimatedFeeEvent()
    data class GetFeeRateSuccess(val estimateFeeRates: EstimateFeeRates) : EstimatedFeeEvent()
    data class EstimatedFeeErrorEvent(val message: String) : EstimatedFeeEvent()
    data class EstimatedFeeCompletedEvent(
        val subtractFeeFromAmount: Boolean,
        val manualFeeRate: Int
    ) : EstimatedFeeEvent()
    data object DraftTransactionSuccess : EstimatedFeeEvent()
}

data class EstimatedFeeState(
    val estimatedFee: Amount = Amount.ZER0,
    val enableSubtractFeeFromAmount: Boolean = true,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeDetails: Boolean = false,
    val estimateFeeRates: EstimateFeeRates = EstimateFeeRates(),
    val allTags: Map<Int, CoinTag> = emptyMap(),
    val allCoins: List<UnspentOutput> = emptyList(),
    val inputs: List<TxInput> = emptyList(),
    val manualFeeRate: Int = estimateFeeRates.defaultRate,
    val cpfpFee: Amount = Amount.ZER0,
    val rollOverWalletPairAmount: PairAmount = PairAmount(Amount.ZER0, Amount.ZER0),
)