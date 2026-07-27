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

package com.nunchuk.android.transaction.components.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveTransactionViewModel @Inject constructor(
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ReceiveTransactionEvent>()
    val event = _event.asSharedFlow()

    private var signerId = ""

    fun checkPassphrase(walletId: String) {
        viewModelScope.launch {
            val wallet = getWalletDetail2UseCase(walletId).getOrNull()
            if (wallet?.needsPassphrase == true) {
                signerId = wallet.signers.firstOrNull()?.masterFingerprint.orEmpty()
                _event.emit(ReceiveTransactionEvent.RequirePassphrase())
            } else {
                _event.emit(ReceiveTransactionEvent.Ready)
            }
        }
    }

    fun sendPassphrase(passphrase: String) {
        if (signerId.isEmpty()) return
        viewModelScope.launch {
            sendSignerPassphraseUseCase(
                SendSignerPassphraseUseCase.Param(signerId = signerId, passphrase = passphrase)
            ).onSuccess {
                _event.emit(ReceiveTransactionEvent.Ready)
            }.onFailure { error ->
                _event.emit(ReceiveTransactionEvent.RequirePassphrase(error.message.orUnknownError()))
            }
        }
    }
}

sealed class ReceiveTransactionEvent {
    data object Ready : ReceiveTransactionEvent()
    data class RequirePassphrase(val errorMessage: String? = null) : ReceiveTransactionEvent()
}
