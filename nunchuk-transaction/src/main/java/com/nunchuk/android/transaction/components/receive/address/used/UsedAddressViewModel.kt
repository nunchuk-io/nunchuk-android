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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel
import com.nunchuk.android.transaction.components.receive.address.used.UsedAddressEvent.GetUsedAddressErrorEvent
import com.nunchuk.android.usecase.GetAddressBalanceUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UsedAddressViewModel @Inject constructor(
    private val getAddressesUseCase: GetAddressesUseCase,
    private val getAddressBalanceUseCase: GetAddressBalanceUseCase
) : NunchukViewModel<UsedAddressState, UsedAddressEvent>() {

    private lateinit var walletId: String

    override val initialState = UsedAddressState()

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddress()
    }

    private fun getUnusedAddress() {
        viewModelScope.launch {
            getAddressesUseCase.execute(walletId = walletId, used = true)
                .onException { event(GetUsedAddressErrorEvent(it.message.orEmpty())) }
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
            updateState { copy(addresses = addressModels) }
        }
    }

}