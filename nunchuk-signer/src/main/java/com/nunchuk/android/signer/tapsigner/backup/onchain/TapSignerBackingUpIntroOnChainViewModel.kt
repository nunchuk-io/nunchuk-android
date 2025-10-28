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

package com.nunchuk.android.signer.tapsigner.backup.onchain

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
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TapSignerBackingUpIntroOnChainViewModel @Inject constructor(
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val getSignerFromTapsignerMasterSignerUseCase: GetSignerFromTapsignerMasterSignerUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val pushEventManager: PushEventManager,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    cardIdManager: CardIdManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: TapSignerBackingUpIntroOnChainFragmentArgs =
        TapSignerBackingUpIntroOnChainFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<TapSignerBackingUpIntroOnChainEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(TapSignerBackingUpIntroOnChainState(""))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(cardId = cardIdManager.getCardId(args.masterSignerId)) }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, cvc: String, index: Int) {
        viewModelScope.launch {
            _event.emit(TapSignerBackingUpIntroOnChainEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(
                GetTapSignerBackupUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    masterSignerId = args.masterSignerId,
                    index = index,
                )
            )
            _event.emit(TapSignerBackingUpIntroOnChainEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyError(result.exceptionOrNull()))
            }
        }
    }

    fun getSignerForOnChain(isoDep: IsoDep, cvc: String, index: Int) {
        viewModelScope.launch {
            _event.emit(TapSignerBackingUpIntroOnChainEvent.NfcLoading(true))
            getSignerFromTapsignerMasterSignerUseCase(
                GetSignerFromTapsignerMasterSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    masterSignerId = args.masterSignerId,
                    index = index,
                    walletType = WalletType.MULTI_SIG
                )
            ).onSuccess { singleSigner ->
                _event.emit(TapSignerBackingUpIntroOnChainEvent.NfcLoading(false))
                singleSigner?.let {
                    _event.emit(TapSignerBackingUpIntroOnChainEvent.ReturnSignerModel(it))
                } ?: run {
                    _event.emit(TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyError(Exception("Signer not found")))
                }
            }.onFailure { error ->
                _event.emit(TapSignerBackingUpIntroOnChainEvent.NfcLoading(false))
                _event.emit(TapSignerBackingUpIntroOnChainEvent.GetTapSignerBackupKeyError(error))
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerBackingUpIntroOnChainEvent.OnContinueClicked)
        }
    }

    fun setKeyVerified(groupId: String, masterSignerId: String) {
        viewModelScope.launch {
            val result = setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    isAppVerified = true
                )
            )
            if (result.isSuccess) {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.KeyVerifiedSuccess)
            } else {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun setReplaceKeyVerified(keyId: String, filePath: String, groupId: String, walletId: String) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = getChecksum(filePath),
                    isAppVerified = false,
                    groupId = groupId,
                    walletId = walletId
                )
            ).onSuccess {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.KeyVerifiedSuccess)
            }.onFailure {
                _event.emit(TapSignerBackingUpIntroOnChainEvent.ShowError(it))
            }
        }
    }

    private fun getChecksum(filePath: String): String {
        return ChecksumUtil.getChecksum(File(filePath).readBytes())
    }
}

data class TapSignerBackingUpIntroOnChainState(val cardId: String)

sealed class TapSignerBackingUpIntroOnChainEvent {
    data object OnContinueClicked : TapSignerBackingUpIntroOnChainEvent()
    data object OnGetSingleWalletDone: TapSignerBackingUpIntroOnChainEvent()
    data object KeyVerifiedSuccess : TapSignerBackingUpIntroOnChainEvent()
    data class NfcLoading(val isLoading: Boolean) : TapSignerBackingUpIntroOnChainEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : TapSignerBackingUpIntroOnChainEvent()
    data class GetTapSignerBackupKeyError(val e: Throwable?) : TapSignerBackingUpIntroOnChainEvent()
    data class ReturnSignerModel(val singleSigner: SingleSigner) : TapSignerBackingUpIntroOnChainEvent()
    data class ShowError(val throwable: Throwable?) : TapSignerBackingUpIntroOnChainEvent()
}

