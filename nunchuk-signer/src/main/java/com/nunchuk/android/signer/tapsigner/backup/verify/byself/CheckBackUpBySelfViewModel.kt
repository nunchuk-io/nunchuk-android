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

package com.nunchuk.android.signer.tapsigner.backup.verify.byself

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckBackUpBySelfViewModel @Inject constructor(
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: CheckBackUpBySelfFragmentArgs =
        CheckBackUpBySelfFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpBySelfEvent>()
    val event = _event.asSharedFlow()

    fun onBtnClicked(event: CheckBackUpBySelfEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    fun setKeyVerified(groupId: String) {
        viewModelScope.launch {
            val result =
                setKeyVerifiedUseCase(SetKeyVerifiedUseCase.Param(groupId, args.masterSignerId, false))
            if (result.isSuccess) {
                _event.emit(OnExitSelfCheck)
            } else {
                _event.emit(ShowError(result.exceptionOrNull()))
            }
        }
    }
}

sealed class CheckBackUpBySelfEvent
object OnDownloadBackUpClicked : CheckBackUpBySelfEvent()
object OnVerifiedBackUpClicked : CheckBackUpBySelfEvent()
object OnExitSelfCheck : CheckBackUpBySelfEvent()
data class ShowError(val e: Throwable?) : CheckBackUpBySelfEvent()