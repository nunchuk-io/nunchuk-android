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

package com.nunchuk.android.main.membership.key.server.limit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.main.R
import com.nunchuk.android.model.SpendingCurrencyUnit
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigSpendingLimitViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = ConfigSpendingLimitFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ConfigSpendingLimitEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfigSpendingLimitState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        _state.update {
            it.copy(
                timeUnit = args.keyPolicy?.spendingPolicy?.timeUnit ?: SpendingTimeUnit.DAILY,
                currencyUnit = args.keyPolicy?.spendingPolicy?.currencyUnit ?: LOCAL_CURRENCY
            )
        }
    }

    fun showTimeUnitOption() {
        viewModelScope.launch {
            _event.emit(ConfigSpendingLimitEvent.ShowTimeUnit)
        }
    }

    fun showCurrencyUnitOption() {
        viewModelScope.launch {
            _event.emit(ConfigSpendingLimitEvent.ShowCurrencyUnit)
        }
    }

    fun onContinueClicked(value: Double) {
        viewModelScope.launch {
            _event.emit(
                ConfigSpendingLimitEvent.ContinueClicked(
                    SpendingPolicy(
                        limit = value,
                        timeUnit = _state.value.timeUnit,
                        currencyUnit = _state.value.currencyUnit,
                    )
                )
            )
        }
    }

    fun setTimeUnit(unit: SpendingTimeUnit) {
        _state.update { it.copy(timeUnit = unit) }
    }

    fun setCurrencyUnit(unit: String) {
        _state.update { it.copy(currencyUnit = unit) }
    }
}

data class ConfigSpendingLimitState(
    val currencyUnit: String = LOCAL_CURRENCY,
    val timeUnit: SpendingTimeUnit = SpendingTimeUnit.DAILY
)

sealed class ConfigSpendingLimitEvent {
    object ShowTimeUnit : ConfigSpendingLimitEvent()
    object ShowCurrencyUnit : ConfigSpendingLimitEvent()
    data class ContinueClicked(val spendingPolicy: SpendingPolicy) : ConfigSpendingLimitEvent()
}

fun SpendingCurrencyUnit.toLabel(context: Context) = when (this) {
    SpendingCurrencyUnit.CURRENCY_UNIT -> LOCAL_CURRENCY
    SpendingCurrencyUnit.BTC -> context.getString(R.string.nc_currency_btc)
    SpendingCurrencyUnit.sat -> context.getString(R.string.nc_currency_sat)
}

fun SpendingTimeUnit.toLabel(context: Context) = when (this) {
    SpendingTimeUnit.DAILY -> context.getString(R.string.nc_daily)
    SpendingTimeUnit.MONTHLY -> context.getString(R.string.nc_monthly)
    SpendingTimeUnit.WEEKLY -> context.getString(R.string.nc_weekly)
    SpendingTimeUnit.YEARLY -> context.getString(R.string.nc_yearly)
}