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

package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownperiod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesLockdownUseCase
import com.nunchuk.android.core.domain.membership.GetLockdownPeriodUseCase
import com.nunchuk.android.core.domain.membership.GetLockdownUserDataUseCase
import com.nunchuk.android.core.domain.membership.LockdownUpdateUseCase
import com.nunchuk.android.core.util.orUnknownError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownPeriodViewModel @Inject constructor(
    private val getLockdownPeriodUseCase: GetLockdownPeriodUseCase,
    private val calculateRequiredSignaturesLockdownUseCase: CalculateRequiredSignaturesLockdownUseCase,
    private val getLockdownUserDataUseCase: GetLockdownUserDataUseCase,
    private val lockdownUpdateUseCase: LockdownUpdateUseCase,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val args = EmergencyLockdownPeriodFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<LockdownPeriodEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(LockdownPeriodState())
    val state = _state.asStateFlow()

    init {
        getLockdownPeriod()
    }

    private fun getLockdownPeriod() = viewModelScope.launch {
        val result = getLockdownPeriodUseCase(Unit)
        if (result.isSuccess) {
            val options = result.getOrNull()?.map {
                PeriodOption(it, isSelected = false)
            }.orEmpty()
            _state.update {
                it.copy(options = options)
            }
        } else {
            _event.emit(LockdownPeriodEvent.ProcessFailure(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun calculateRequiredSignatures() = viewModelScope.launch {
        _event.emit(LockdownPeriodEvent.Loading(true))
        val period = _state.value.options.first { it.isSelected }.period
        val resultCalculate = calculateRequiredSignaturesLockdownUseCase(
            CalculateRequiredSignaturesLockdownUseCase.Param(
                walletId = "",
                periodId = period.id
            )
        )
        val resultUserData = getLockdownUserDataUseCase(
            GetLockdownUserDataUseCase.Param(
                walletId = "",
                periodId = period.id
            )
        )
        val userData = resultUserData.getOrThrow()
        _state.update {
            it.copy(userData = userData, period = period)
        }
        _event.emit(LockdownPeriodEvent.Loading(false))
        if (resultCalculate.isSuccess) {
            _event.emit(
                LockdownPeriodEvent.CalculateRequiredSignaturesSuccess(
                    type = resultCalculate.getOrThrow().type,
                    walletId = "",
                    userData = userData,
                    requiredSignatures = resultCalculate.getOrThrow().requiredSignatures
                )
            )
        } else {
            _event.emit(LockdownPeriodEvent.ProcessFailure(resultCalculate.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun lockdownUpdate(signatures: HashMap<String, String>, securityQuestionToken: String) =
        viewModelScope.launch {
            val state = _state.value
            _event.emit(LockdownPeriodEvent.Loading(true))
            val result = lockdownUpdateUseCase(
                LockdownUpdateUseCase.Param(
                    signatures = signatures,
                    verifyToken = args.verifyToken,
                    userData = state.userData.orEmpty(),
                    securityQuestionToken = securityQuestionToken
                )
            )
            _event.emit(LockdownPeriodEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(LockdownPeriodEvent.LockdownUpdateSuccess(state.period?.displayName.orEmpty()))
            } else {
                _event.emit(LockdownPeriodEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

    fun onOptionClick(id: String) {
        val value = _state.value
        val options = value.options.toMutableList()
        val newOptions = options.map {
            it.copy(isSelected = it.period.id == id)
        }
        _state.update {
            it.copy(options = newOptions)
        }
    }
}
