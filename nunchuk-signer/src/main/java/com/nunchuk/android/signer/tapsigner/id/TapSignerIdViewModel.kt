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

package com.nunchuk.android.signer.tapsigner.id

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerIdViewModel @Inject constructor(
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args : TapSignerIdFragmentArgs = TapSignerIdFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<TapSignerIdEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            val result = getTapSignerStatusByIdUseCase(args.masterSignerId)
            if (result.isSuccess) {
                _state.update { it.copy(cardId = result.getOrThrow().ident.orEmpty()) }
            }
        }
    }

    private val _state = MutableStateFlow(TapSignerIdState(""))
    val state = _state.asStateFlow()


    fun getTapSignerBackup(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc, args.masterSignerId))
            _event.emit(TapSignerIdEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyError(result.exceptionOrNull()))
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.OnContinueClicked)
        }
    }

    fun onAddNewOneClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.OnAddNewOne)
        }
    }
}

data class TapSignerIdState(val cardId: String)

sealed class TapSignerIdEvent {
    object OnContinueClicked : TapSignerIdEvent()
    object OnAddNewOne : TapSignerIdEvent()
    data class NfcLoading(val isLoading: Boolean) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyError(val e: Throwable?) : TapSignerIdEvent()
}