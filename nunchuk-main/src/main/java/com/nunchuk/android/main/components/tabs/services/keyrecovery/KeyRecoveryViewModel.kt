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

package com.nunchuk.android.main.components.tabs.services.keyrecovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryViewModel @Inject constructor(
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val args = KeyRecoveryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<KeyRecoveryEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(KeyRecoveryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    _state.update { it.copy(plans = plans, myUserRole = args.role) }
                    _state.update { it.copy(actionItems = it.initRowItems()) }
                }
        }
    }

    fun onItemClick(item: KeyRecoveryActionItem) = viewModelScope.launch {
        _event.emit(KeyRecoveryEvent.ItemClick(item))
    }

    fun confirmPassword(password: String, item: KeyRecoveryActionItem) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        _event.emit(KeyRecoveryEvent.Loading(true))
        val targetAction = when (item) {
            is KeyRecoveryActionItem.StartKeyRecovery -> {
                TargetAction.DOWNLOAD_KEY_BACKUP.name
            }
            is KeyRecoveryActionItem.UpdateRecoveryQuestion -> {
                TargetAction.UPDATE_SECURITY_QUESTIONS.name
            }
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                targetAction = targetAction,
                password = password
            )
        )
        _event.emit(KeyRecoveryEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(KeyRecoveryEvent.CheckPasswordSuccess(item, result.getOrThrow().orEmpty()))
        } else {
            _event.emit(KeyRecoveryEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}