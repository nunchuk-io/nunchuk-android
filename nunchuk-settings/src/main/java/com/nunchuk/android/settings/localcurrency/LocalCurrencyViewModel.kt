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

package com.nunchuk.android.settings.localcurrency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetForexCurrenciesUseCase
import com.nunchuk.android.core.domain.SetLocalCurrencyUseCase
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalCurrencyViewModel @Inject constructor(
    private val getLocalCurrencyUseCase: GetLocalCurrencyUseCase,
    private val setCurrencyUseCase: SetLocalCurrencyUseCase,
    private val getForexCurrenciesUseCase: GetForexCurrenciesUseCase,
) :
    ViewModel() {

    private val _event = MutableSharedFlow<LocalCurrencyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(LocalCurrencyState())
    val state = _state.asStateFlow()

    init {
        getLocalCurrency()
        getCurrencyList()
    }

    private fun getCurrencyList() = viewModelScope.launch {
        val result = getForexCurrenciesUseCase(Unit)
        if (result.isSuccess) {
            _state.update { it.copy(currencies = result.getOrThrow()) }
        }
    }

    private fun getLocalCurrency() = viewModelScope.launch {
        val result = getLocalCurrencyUseCase(Unit).first()
        _state.update {
            it.copy(
                currentCurrency = result.getOrDefault(""),
                selectedCurrency = result.getOrDefault("")
            )
        }
    }

    fun selectCurrency(unit: String) {
        _state.update { it.copy(selectedCurrency = unit) }
    }

    fun onSaveClick() = viewModelScope.launch {
        val result = setCurrencyUseCase(SetLocalCurrencyUseCase.Params(
            fromCurrency = _state.value.currentCurrency,
            toCurrency = _state.value.selectedCurrency,
        ))
        if (result.isSuccess) {
            LOCAL_CURRENCY = _state.value.selectedCurrency
            _event.emit(LocalCurrencyEvent.SetLocalCurrencySuccess)
        } else {
            _event.emit(LocalCurrencyEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}

sealed class LocalCurrencyEvent {
    object SetLocalCurrencySuccess : LocalCurrencyEvent()
    data class Error(val message: String) : LocalCurrencyEvent()
}

data class LocalCurrencyState(
    val currencies: LinkedHashMap<String, String> = linkedMapOf(),
    val selectedCurrency: String = "",
    val currentCurrency: String = ""
)