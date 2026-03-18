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

package com.nunchuk.android.transaction.components.receive.address.used

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel
import com.nunchuk.android.usecase.GetAddressBalanceUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UsedAddressViewModel @Inject constructor(
    private val getAddressesUseCase: GetAddressesUseCase,
    private val getAddressBalanceUseCase: GetAddressBalanceUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(UsedAddressState())
    val state: StateFlow<UsedAddressState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<UsedAddressEvent>()
    val event: SharedFlow<UsedAddressEvent> = _event.asSharedFlow()

    private lateinit var walletId: String

    init {
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.ReloadUsedAddress -> getUsedAddresses()
                    else -> {}
                }
            }
        }
    }

    fun init(walletId: String) {
        this.walletId = walletId
        getUsedAddresses()
    }

    private fun getUsedAddresses() {
        viewModelScope.launch {
            getAddressesUseCase.execute(walletId = walletId, used = true)
                .onException { _event.emit(UsedAddressEvent.GetUsedAddressErrorEvent(it.message.orEmpty())) }
                .collect { getAddressBalance(it) }
        }
    }

    private fun getAddressBalance(addresses: List<String>) {
        viewModelScope.launch {
            val addressModels = addresses.map {
                when (val result = getAddressBalanceUseCase.execute(walletId, it)) {
                    is Success -> UsedAddressModel(it, result.data)
                    is Error -> UsedAddressModel(it, Amount.ZER0)
                }
            }
            _state.update { it.copy(addresses = addressModels) }
        }
    }
}
