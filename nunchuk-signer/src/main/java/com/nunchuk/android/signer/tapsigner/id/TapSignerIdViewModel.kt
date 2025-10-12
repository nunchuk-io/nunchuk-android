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

package com.nunchuk.android.signer.tapsigner.id

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.WalletType
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
class TapSignerIdViewModel @Inject constructor(
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val getSignerFromTapsignerMasterSignerUseCase: GetSignerFromTapsignerMasterSignerUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val pushEventManager: PushEventManager,
    cardIdManager: CardIdManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: TapSignerIdFragmentArgs =
        TapSignerIdFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<TapSignerIdEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(TapSignerIdState(""))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(cardId = cardIdManager.getCardId(args.masterSignerId)) }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, cvc: String, index: Int) {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(
                GetTapSignerBackupUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    masterSignerId = args.masterSignerId,
                    index = index,
                )
            )
            _event.emit(TapSignerIdEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyError(result.exceptionOrNull()))
            }
        }
    }

    fun getSignerFromTapsignerMasterSigner(isoDep: IsoDep, cvc: String, index: Int, walletId: String) {
        viewModelScope.launch {
            if (walletId.isNotEmpty()) {
                getWalletDetail2UseCase(walletId).onSuccess {
                    val walletType =
                        if (it.signers.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
                    getSignerSigner(isoDep, cvc, index, walletType)
                }
            } else {
                getSignerSigner(isoDep, cvc, index, WalletType.MULTI_SIG)
            }
        }
    }

    private suspend fun getSignerSigner(
        isoDep: IsoDep,
        cvc: String,
        index: Int,
        walletType: WalletType
    ) {
        getSignerFromTapsignerMasterSignerUseCase(
            GetSignerFromTapsignerMasterSignerUseCase.Data(
                isoDep = isoDep,
                cvc = cvc,
                masterSignerId = args.masterSignerId,
                index = index,
                walletType = walletType
            )
        ).onSuccess { singleSigner ->
            singleSigner?.let {
                pushEventManager.push(PushEvent.LocalUserSignerAdded(singleSigner))
            }
            _event.emit(TapSignerIdEvent.OnGetSingleWalletDone)
        }
    }


    fun getSignerForOnChain(isoDep: IsoDep, cvc: String, index: Int) {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.NfcLoading(true))
            getSignerFromTapsignerMasterSignerUseCase(
                GetSignerFromTapsignerMasterSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    masterSignerId = args.masterSignerId,
                    index = index,
                    walletType = WalletType.MULTI_SIG
                )
            ).onSuccess { singleSigner ->
                _event.emit(TapSignerIdEvent.NfcLoading(false))
                singleSigner?.let {
                    _event.emit(TapSignerIdEvent.ReturnSignerModel(it))
                } ?: run {
                    _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyError(Exception("Signer not found")))
                }
            }.onFailure { error ->
                _event.emit(TapSignerIdEvent.NfcLoading(false))
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyError(error))
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
    data object OnContinueClicked : TapSignerIdEvent()
    data object OnAddNewOne : TapSignerIdEvent()
    data object OnGetSingleWalletDone: TapSignerIdEvent()
    data class NfcLoading(val isLoading: Boolean) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyError(val e: Throwable?) : TapSignerIdEvent()
    data class ReturnSignerModel(val singleSigner: SingleSigner) : TapSignerIdEvent()
}