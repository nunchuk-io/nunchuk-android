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

package com.nunchuk.android.settings.walletsecurity.createpin

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.settings.R
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.pin.ChangeDecoyPinUseCase
import com.nunchuk.android.usecase.pin.DecoyPinExistUseCase
import com.nunchuk.android.usecase.pin.SetCustomPinConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletSecurityCreatePinViewModel @Inject constructor(
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val decoyPinExistUseCase: DecoyPinExistUseCase,
    private val changeDecoyPinUseCase: ChangeDecoyPinUseCase,
    private val accountManager: AccountManager,
    private val setCustomPinConfigUseCase: SetCustomPinConfigUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = WalletSecurityCreatePinFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<WalletSecurityCreatePinEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(WalletSecurityCreatePinState())
    val state = _state.asStateFlow()

    private val createPinFlow = args.isEnable.not()
    private val account = accountManager.getAccount()
    private val decoyPin = account.decoyPin

    init {
        val inputValue = hashMapOf<Int, InputValue>()
        repeat((0..if (createPinFlow) 1 else 2).count()) {
            inputValue[it] = InputValue()
        }
        _state.update {
            it.copy(
                inputValue = inputValue,
                createPinFlow = createPinFlow
            )
        }
    }

    fun createOrUpdateWalletPin() {
        if (decoyPin.isNotEmpty()) {
            changeDecoyPin()
        } else {
            createOrUpdateAppPin()
        }
    }

    /**
     * Flow decoy space
     */
    private fun changeDecoyPin() {
        viewModelScope.launch {
            val inputValue = _state.value.inputValue
            if (_state.value.createPinFlow) {
                if (inputValue[0]?.value == inputValue[1]?.value) {
                    changeDecoyPin(inputValue)
                } else {
                    updateInputValue(
                        1,
                        inputValue[1]?.value!!,
                        errorMsg = context.getString(R.string.nc_confirm_pin_does_not_match)
                    )
                }
            } else {
                val matchPin = inputValue[0]!!.value == decoyPin
                val decoyPinExist = decoyPinExistUseCase(inputValue[1]!!.value)
                if (decoyPinExist.getOrDefault(false)) {
                    updateInputValue(
                        0,
                        inputValue[0]?.value!!,
                        errorMsg = context.getString(R.string.nc_pin_exist)
                    )
                } else if (matchPin.not()) {
                    updateInputValue(
                        0,
                        inputValue[0]?.value!!,
                        errorMsg = context.getString(R.string.nc_incorrect_current_pin)
                    )
                    _state.update { it.copy(attemptCount = it.attemptCount.inc()) }
                } else if (inputValue[1] != inputValue[2]) {
                    updateInputValue(
                        2,
                        inputValue[2]?.value!!,
                        errorMsg = context.getString(R.string.nc_confirm_pin_does_not_match)
                    )
                } else {
                    changeDecoyPin(inputValue)
                }
            }
        }
    }

    private suspend fun changeDecoyPin(inputValue: MutableMap<Int, InputValue>) {
        val newDecoyPin = inputValue[1]!!.value
        changeDecoyPinUseCase(
            ChangeDecoyPinUseCase.Params(
                oldPin = decoyPin,
                newPin = newDecoyPin
            )
        ).onSuccess {
            if (it) {
                accountManager.storeAccount(account.copy(decoyPin = newDecoyPin))
                setCustomPinConfigUseCase(
                    SetCustomPinConfigUseCase.Params(
                        decoyPin = newDecoyPin,
                        isEnable = true
                    )
                )
                initNunchukUseCase(
                    InitNunchukUseCase.Param(
                        accountId = "",
                        decoyPin = newDecoyPin
                    )
                )
            }
            _event.emit(WalletSecurityCreatePinEvent.CreateOrUpdateSuccess)
        }.onFailure {
            updateInputValue(0, inputValue[0]?.value!!, errorMsg = it.message.orUnknownError())
        }
    }

    private fun createOrUpdateAppPin() {
        viewModelScope.launch {
            val inputValue = _state.value.inputValue
            if (_state.value.createPinFlow) {
                var errorMsg = ""
                if (inputValue[0]?.value == inputValue[1]?.value) {
                    createOrUpdateWalletPinUseCase(inputValue[0]!!.value)
                    _event.emit(WalletSecurityCreatePinEvent.CreateOrUpdateSuccess)
                } else {
                    errorMsg = context.getString(R.string.nc_confirm_pin_does_not_match)
                }
                updateInputValue(1, inputValue[1]?.value!!, errorMsg = errorMsg)
            } else {
                val matchPin = checkWalletPinUseCase(inputValue[0]!!.value)
                val decoyPinExist = decoyPinExistUseCase(inputValue[1]!!.value)
                if (decoyPinExist.getOrDefault(false)) {
                    updateInputValue(
                        0,
                        inputValue[0]?.value!!,
                        errorMsg = context.getString(R.string.nc_pin_exist)
                    )
                } else if (matchPin.getOrDefault(false).not()) {
                    updateInputValue(
                        0,
                        inputValue[0]?.value!!,
                        errorMsg = context.getString(R.string.nc_incorrect_current_pin)
                    )
                    _state.update { it.copy(attemptCount = it.attemptCount.inc()) }
                } else if (inputValue[1] != inputValue[2]) {
                    updateInputValue(
                        2,
                        inputValue[2]?.value!!,
                        errorMsg = context.getString(R.string.nc_confirm_pin_does_not_match)
                    )
                } else {
                    createOrUpdateWalletPinUseCase(inputValue[1]!!.value)
                    _event.emit(WalletSecurityCreatePinEvent.CreateOrUpdateSuccess)
                }
            }
        }
    }

    fun updateInputValue(index: Int, value: String, errorMsg: String = "") {
        val inputValue = _state.value.inputValue.toMutableMap()
        val currentInputValue = inputValue[index]
        inputValue[index] = currentInputValue!!.copy(value = value, errorMsg = errorMsg)
        _state.update { it.copy(inputValue = inputValue) }
    }
}

data class InputValue(
    val value: String = "",
    val errorMsg: String = ""
)

data class WalletSecurityCreatePinState(
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    val inputValue: MutableMap<Int, InputValue> = hashMapOf(),
    val createPinFlow: Boolean = false,
    val attemptCount: Int = 0
)

sealed class WalletSecurityCreatePinEvent {
    data class Loading(val loading: Boolean) : WalletSecurityCreatePinEvent()
    data class Error(val message: String) : WalletSecurityCreatePinEvent()
    object CreateOrUpdateSuccess : WalletSecurityCreatePinEvent()
}