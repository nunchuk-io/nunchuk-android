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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.SignerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryTapSignerListBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args =
        RecoveryTapSignerListBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<RecoveryTapSignerListBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RecoveryTapSignerListBottomSheetState())
    val state = _state.asStateFlow()

    fun onSignerSelected(signer: SignerModel) = viewModelScope.launch {
        _state.update {
            it.copy(selectedSignerId = signer.id)
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        _event.emit(RecoveryTapSignerListBottomSheetEvent.ContinueClick)
    }

    val selectedSigner: SignerModel?
        get() = args.signers.find { it.id == _state.value.selectedSignerId }

}