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

package com.nunchuk.android.main.membership.key.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.SignerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSingerListBottomSheetViewModel @Inject constructor(
) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerListBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private val _selectSingle = MutableStateFlow<SignerModel?>(null)
    val selectSingle = _selectSingle.asStateFlow()

    fun onSignerSelected(signer: SignerModel) {
        _selectSingle.value = signer
    }

    fun onAddExistingKey() {
        viewModelScope.launch {
            _selectSingle.value?.let {
                _event.emit(TapSignerListBottomSheetEvent.OnAddExistingKey(it))
            }
        }
    }

    fun onAddNewKey() {
        viewModelScope.launch {
            _event.emit(TapSignerListBottomSheetEvent.OnAddNewKey)
        }
    }
}

sealed class TapSignerListBottomSheetEvent {
    data class OnAddExistingKey(val signer: SignerModel) : TapSignerListBottomSheetEvent()
    object OnAddNewKey : TapSignerListBottomSheetEvent()
}