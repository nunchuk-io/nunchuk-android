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

package com.nunchuk.android.main.membership.honey.registerwallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.COLDCARD_DEFAULT_KEY_NAME
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.user.SetRegisterColdcardUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterWalletToColdcardViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val setRegisterColdcardUseCase: SetRegisterColdcardUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _event = MutableSharedFlow<RegisterWalletToColdcardEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RegisterWalletToColdcardUiState())
    val state = _state.asStateFlow()

    private val args: RegisterWalletToColdcardFragmentArgs =
        RegisterWalletToColdcardFragmentArgs.fromSavedStateHandle(savedStateHandle)

    val remainTime = membershipStepManager.remainingTime

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId)
                .onSuccess {
                    val keyName =
                        it.signers.filter { signer -> signer.type == SignerType.COLDCARD_NFC }
                            .reversed()
                            .getOrNull(args.index.dec())?.name ?: COLDCARD_DEFAULT_KEY_NAME
                    _state.update { state -> state.copy(keyName = keyName) }
                }
        }
    }

    fun onExportColdcardClicked() {
        viewModelScope.launch {
            _event.emit(RegisterWalletToColdcardEvent.ExportWalletToColdcard)
        }
    }

    fun setRegisterColdcardSuccess(walletId: String) {
        viewModelScope.launch {
            setRegisterColdcardUseCase(SetRegisterColdcardUseCase.Params(walletId, -1))
        }
    }
}

data class RegisterWalletToColdcardUiState(val keyName: String = "")

sealed class RegisterWalletToColdcardEvent {
    object ExportWalletToColdcard : RegisterWalletToColdcardEvent()
}