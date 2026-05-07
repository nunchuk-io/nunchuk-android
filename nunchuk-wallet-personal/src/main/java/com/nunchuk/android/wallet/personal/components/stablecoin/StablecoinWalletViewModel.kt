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

package com.nunchuk.android.wallet.personal.components.stablecoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.signer.GetMasterSigners2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StablecoinWalletViewModel @Inject constructor(
    private val getMasterSigners2UseCase: GetMasterSigners2UseCase,
    private val masterSignerMapper: MasterSignerMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(StablecoinWalletState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getMasterSigners2UseCase(Unit).onSuccess { masters ->
                val softwareSigners = masters
                    .filter { it.type == SignerType.SOFTWARE }
                    .map { masterSignerMapper(it) }
                _state.update { it.copy(softwareSigners = softwareSigners) }
            }
        }
    }
}

data class StablecoinWalletState(
    val softwareSigners: List<SignerModel> = emptyList(),
)
