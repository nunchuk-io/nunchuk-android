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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterWalletToAirgapViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<RegisterWalletToAirgapEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onExportAirgapClicked() {
        viewModelScope.launch {
            _event.emit(RegisterWalletToAirgapEvent.ExportWalletToAirgap)
        }
    }

    fun setRegisterAirgapSuccess(walletId: String) {
        viewModelScope.launch {
            setRegisterAirgapUseCase(SetRegisterAirgapUseCase.Params(walletId, -1))
        }
    }
}

sealed class RegisterWalletToAirgapEvent {
    object ExportWalletToAirgap : RegisterWalletToAirgapEvent()
}