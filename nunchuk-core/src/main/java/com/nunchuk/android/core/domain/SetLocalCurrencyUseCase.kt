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

package com.nunchuk.android.core.domain

import com.nunchuk.android.core.repository.BtcRepository
import com.nunchuk.android.core.util.CurrencyUtil
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SetLocalCurrencyUseCase @Inject constructor(
    private val settingRepository: SettingRepository,
    private val btcRepository: BtcRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<SetLocalCurrencyUseCase.Params, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Params) {
        settingRepository.setLocalCurrency(parameters.toCurrency)
        updateCurrencyTaprootFeeSelection(parameters)
    }

    private suspend fun updateCurrencyTaprootFeeSelection(params: Params) {
        val taprootFeeSelectionSetting = settingRepository.getTaprootFeeSelection().first()
        val forexRates = btcRepository.getForexRates()
        val newValueCurrency = CurrencyUtil.convertCurrency(
            fromCurrency = params.fromCurrency,
            toCurrency = params.toCurrency,
            amount = taprootFeeSelectionSetting.feeDifferenceThresholdCurrency,
            rates = forexRates
        )
        settingRepository.setTaprootFeeSelection(taprootFeeSelectionSetting.copy(
            feeDifferenceThresholdCurrency = newValueCurrency,
        ))
    }

    data class Params(
        val fromCurrency: String,
        val toCurrency: String,
    )
}