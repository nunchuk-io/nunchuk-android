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

package com.nunchuk.android.wallet.personal.components.stablecoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.signer.GetMasterSigners2UseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import com.nunchuk.android.usecase.wallet.CreateUsdtWalletFromSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StablecoinWalletViewModel @Inject constructor(
    private val getMasterSigners2UseCase: GetMasterSigners2UseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val createUsdtWalletFromSignerUseCase: CreateUsdtWalletFromSignerUseCase,
    pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(StablecoinWalletState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<StablecoinWalletEvent>()
    val event = _event.asSharedFlow()

    private val masterSigners = mutableListOf<MasterSigner>()

    init {
        viewModelScope.launch {
            getMasterSigners2UseCase(Unit).onSuccess { masters ->
                val software = masters.filter { it.type == SignerType.SOFTWARE }
                masterSigners.clear()
                masterSigners.addAll(software)
                _state.update { it.copy(softwareSigners = software.map { s -> masterSignerMapper(s) }) }
            }
        }
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                .collect { createUsdtWallet(it.signer) }
        }
    }

    fun createWalletFromExistingSigner(signerModel: SignerModel) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
            if (masterSigner == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            getUnusedSignerFromMasterSignerV2UseCase(
                GetUnusedSignerFromMasterSignerV2UseCase.Params(
                    masterSigner,
                    WalletType.SINGLE_SIG,
                    AddressType.NATIVE_SEGWIT,
                )
            ).onSuccess { singleSigner ->
                createUsdtWallet(singleSigner)
            }.onFailure {
                _state.update { it.copy(isLoading = false) }
                _event.emit(StablecoinWalletEvent.Error(it.message.orUnknownError()))
            }
        }
    }

    private fun createUsdtWallet(signer: SingleSigner) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            createUsdtWalletFromSignerUseCase(signer)
                .onSuccess { wallet ->
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(StablecoinWalletEvent.WalletCreated(wallet.id))
                }.onFailure {
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(StablecoinWalletEvent.Error(it.message.orUnknownError()))
                }
        }
    }
}

data class StablecoinWalletState(
    val softwareSigners: List<SignerModel> = emptyList(),
    val isLoading: Boolean = false,
)

sealed class StablecoinWalletEvent {
    data class WalletCreated(val walletId: String) : StablecoinWalletEvent()
    data class Error(val message: String) : StablecoinWalletEvent()
}
