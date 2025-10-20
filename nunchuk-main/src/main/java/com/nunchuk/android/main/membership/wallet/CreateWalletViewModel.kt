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

package com.nunchuk.android.main.membership.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CreatePersonalWalletUseCase
import com.nunchuk.android.core.util.isColdCard
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.user.SetRegisterAirgapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWalletViewModel @Inject constructor(
    private val createPersonalWalletUseCase: CreatePersonalWalletUseCase,
    private val setRegisterAirgapUseCase: SetRegisterAirgapUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _event = MutableSharedFlow<CreateWalletEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CreateWalletState.EMPTY)
    val state = _state.asStateFlow()

    val walletId: String
        get() = savedStateHandle.get<String?>("wallet_id").orEmpty()

    private var createWalletJob: Job? = null

    fun updateWalletName(walletName: String) {
        _state.update {
            it.copy(walletName = walletName)
        }
    }

    fun createQuickWallet(sendBsmsEmail: Boolean) {
        if (createWalletJob?.isActive == true) return
        createWalletJob = viewModelScope.launch {
            _event.emit(CreateWalletEvent.Loading(true))
            createPersonalWalletUseCase(
                CreatePersonalWalletUseCase.Param(
                    name = _state.value.walletName,
                    sendBsmsEmail = sendBsmsEmail
                )
            ).onFailure {
                _event.emit(CreateWalletEvent.ShowError(it.message.orUnknownError()))
            }.onSuccess { result ->
                savedStateHandle["wallet_id"] = result.wallet.id
                val totalAirgap =
                    result.wallet.signers.count { signer -> signer.type == SignerType.AIRGAP && !signer.isColdCard }
                if (totalAirgap > 0) {
                    setRegisterAirgapUseCase(
                        SetRegisterAirgapUseCase.Params(
                            result.wallet.id,
                            totalAirgap
                        )
                    )
                }
                if (result.requiresRegistration) {
                    _event.emit(CreateWalletEvent.OpenUploadConfigurationScreen(result.wallet.id))
                } else {
                    _event.emit(
                        CreateWalletEvent.OnCreateWalletSuccess(
                            wallet = result.wallet,
                            airgapCount = totalAirgap,
                            sendBsmsEmail = sendBsmsEmail
                        )
                    )
                }
            }
            _event.emit(CreateWalletEvent.Loading(false))

        }
    }
}