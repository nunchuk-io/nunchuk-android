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

package com.nunchuk.android.signer.components.details

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateColdCardHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.HealthCheckColdCardUseCase
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.domain.HealthCheckTapSignerUseCase
import com.nunchuk.android.core.domain.TopUpXpubTapSignerUseCase
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.domain.membership.UpdateExistingKeyUseCase
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.components.details.SignerInfoEvent.GenerateColdcardHealthMessagesSuccess
import com.nunchuk.android.signer.components.details.SignerInfoEvent.GetTapSignerBackupKeyEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.HealthCheckErrorEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.HealthCheckSuccessEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.NfcError
import com.nunchuk.android.signer.components.details.SignerInfoEvent.NfcLoading
import com.nunchuk.android.signer.components.details.SignerInfoEvent.RemoveSignerCompletedEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.RemoveSignerErrorEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.TopUpXpubFailed
import com.nunchuk.android.signer.components.details.SignerInfoEvent.TopUpXpubSuccess
import com.nunchuk.android.signer.components.details.SignerInfoEvent.UpdateNameErrorEvent
import com.nunchuk.android.signer.components.details.SignerInfoEvent.UpdateNameSuccessEvent
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.DeleteRemoteSignerUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphrase
import com.nunchuk.android.usecase.UpdateMasterSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.GetAssistedKeysUseCase
import com.nunchuk.android.usecase.membership.UpdateServerKeyNameUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SignerInfoViewModel @Inject constructor(
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
    private val deleteRemoteSignerUseCase: DeleteRemoteSignerUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val healthCheckMasterSignerUseCase: HealthCheckMasterSignerUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val healthCheckTapSignerUseCase: HealthCheckTapSignerUseCase,
    private val topUpXpubTapSignerUseCase: TopUpXpubTapSignerUseCase,
    private val cardIdManager: CardIdManager,
    private val generateColdCardHealthCheckMessageUseCase: GenerateColdCardHealthCheckMessageUseCase,
    private val healthCheckColdCardUseCase: HealthCheckColdCardUseCase,
    private val updateServerKeyNameUseCase: UpdateServerKeyNameUseCase,
    private val updateExistingKeyUseCase: UpdateExistingKeyUseCase,
    savedStateHandle: SavedStateHandle,
    getAssistedKeysUseCase: GetAssistedKeysUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SignerInfoState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SignerInfoEvent>()
    val event = _event.asSharedFlow()
    
    private fun getState() = _state.value

    private val assistedKeys = getAssistedKeysUseCase(Unit)
        .map { it.getOrDefault(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val args: SignerInfoFragmentArgs =
        SignerInfoFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            if (args.isMasterSigner) {
                val result = getMasterSignerUseCase(args.id)
                if (result.isSuccess) {

                    _state.update { state -> state.copy(masterSigner = result.getOrThrow()) }
                } else {
                    Timber.e("Get software signer error")
                }
            } else {
                val result = getRemoteSignerUseCase(
                    GetRemoteSignerUseCase.Data(
                        args.masterFingerprint,
                        args.derivationPath
                    )
                )
                if (result.isSuccess) {
                    _state.update { state -> state.copy(remoteSigner = result.getOrThrow()) }
                }
            }
        }
        if (args.signerType == SignerType.NFC) {
            viewModelScope.launch {
                val cardId = cardIdManager.getCardId(args.id)
                _state.update { state -> state.copy(nfcCardId = cardId) }
            }
        }
    }

    fun handleEditCompletedEvent(updateSignerName: String) {
        viewModelScope.launch {
            val state = getState()
            if (args.isMasterSigner) {
                state.masterSigner?.let { signer ->
                    updateMasterSignerUseCase(parameters = signer.copy(name = updateSignerName))
                        .onSuccess {
                            _event.emit(UpdateNameSuccessEvent(updateSignerName))
                            updateServerKeyName(signer.id, updateSignerName)
                        }
                        .onFailure { e ->
                            _event.emit(UpdateNameErrorEvent(e.message.orUnknownError()))
                        }
                }
            } else {
                state.remoteSigner?.let { signer ->
                    when (val result =
                        updateRemoteSignerUseCase.execute(signer = signer.copy(name = updateSignerName))) {
                        is Success -> {
                            _event.emit(UpdateNameSuccessEvent(updateSignerName))
                            updateServerKeyName(signer.masterFingerprint, updateSignerName)
                        }

                        is Error -> _event.emit(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    private fun updateServerKeyName(xfp: String, name: String) {
        viewModelScope.launch {
            if (assistedKeys.value.contains(xfp)) {
                updateServerKeyNameUseCase(UpdateServerKeyNameUseCase.Param(xfp, name))
            }
        }
    }

    fun handleRemoveSigner() {
        viewModelScope.launch {
            val state = getState()
            if (args.isMasterSigner) {
                state.masterSigner?.let {
                    deleteMasterSignerUseCase(it.id)
                        .onSuccess {
                            _event.emit(RemoveSignerCompletedEvent)
                        }.onFailure { exception ->
                            _event.emit(RemoveSignerErrorEvent(exception.message.orUnknownError()))
                        }
                }
            } else {
                state.remoteSigner?.let {
                    deleteRemoteSignerUseCase(DeleteRemoteSignerUseCase.Params(
                        masterFingerprint = it.masterFingerprint,
                        derivationPath = it.derivationPath
                    )).onSuccess {
                        _event.emit(RemoveSignerCompletedEvent)
                    }.onFailure { exception ->
                        _event.emit(RemoveSignerErrorEvent(exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun handleHealthCheck(masterSigner: MasterSigner, passPhrase: String? = null) {
        if (passPhrase != null) {
            viewModelScope.launch {
                sendSignerPassphrase.execute(masterSigner.id, passPhrase)
                    .flowOn(Dispatchers.IO)
                    .onException { _event.emit(HealthCheckErrorEvent(it.message.orEmpty())) }
                    .flowOn(Dispatchers.Main)
                    .collect { healthCheck(masterSigner) }
            }
        } else {
            healthCheck(masterSigner)
        }
    }

    private fun healthCheck(masterSigner: MasterSigner) {
        viewModelScope.launch {
            healthCheckMasterSignerUseCase.execute(
                fingerprint = masterSigner.device.masterFingerprint,
                message = "",
                signature = "",
                path = masterSigner.device.path,
                masterSignerId = if (masterSigner.device.needPassPhraseSent) masterSigner.id else null
            )
                .flowOn(Dispatchers.IO)
                .onException { _event.emit(HealthCheckErrorEvent(it.message)) }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (it == HealthStatus.SUCCESS) {
                        _event.emit(HealthCheckSuccessEvent)
                    } else {
                        _event.emit(HealthCheckErrorEvent())
                    }
                }
        }
    }

    fun healthCheckTapSigner(isoDep: IsoDep, cvc: String, masterSigner: MasterSigner) {
        viewModelScope.launch {
            _event.emit(NfcLoading)
            val result = healthCheckTapSignerUseCase(
                HealthCheckTapSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    fingerprint = masterSigner.device.masterFingerprint,
                    message = "",
                    signature = "",
                    path = masterSigner.device.path
                )
            )
            if (result.isSuccess && result.getOrThrow() == HealthStatus.SUCCESS) {
                _event.emit(HealthCheckSuccessEvent)
            } else {
                _event.emit(HealthCheckErrorEvent(e = result.exceptionOrNull()))
            }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, cvc: String) {
        val masterSignerId = state.value.masterSigner?.id ?: return
        viewModelScope.launch {
            _event.emit(NfcLoading)
            val result = getTapSignerBackupUseCase(
                GetTapSignerBackupUseCase.Data(
                    isoDep,
                    cvc,
                    masterSignerId
                )
            )
            if (result.isSuccess) {
                _event.emit(GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(NfcError(result.exceptionOrNull()))
            }
        }
    }

    fun topUpXpubTapSigner(isoDep: IsoDep, cvc: String, masterSignerId: String) {
        viewModelScope.launch {
            _event.emit(NfcLoading)
            val result = topUpXpubTapSignerUseCase(
                TopUpXpubTapSignerUseCase.Data(
                    isoDep,
                    cvc,
                    masterSignerId
                )
            )
            if (result.isSuccess) {
                _event.emit(TopUpXpubSuccess)
            } else {
                _event.emit(TopUpXpubFailed(result.exceptionOrNull()))
            }
        }
    }

    fun generateColdcardHealthMessages(ndef: Ndef?, derivationPath: String) {
        ndef ?: return
        viewModelScope.launch {
            _event.emit(NfcLoading)
            val result = generateColdCardHealthCheckMessageUseCase(
                GenerateColdCardHealthCheckMessageUseCase.Data(
                    derivationPath = derivationPath,
                    ndef = ndef
                )
            )
            if (result.isSuccess) {
                _event.emit(GenerateColdcardHealthMessagesSuccess)
            } else {
                _event.emit(NfcError(result.exceptionOrNull()))
            }
        }
    }

    fun healthCheckColdCard(signer: SingleSigner, records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(NfcLoading)
            val result =
                healthCheckColdCardUseCase(HealthCheckColdCardUseCase.Param(signer, records))
            if (result.isSuccess) {
                _event.emit(HealthCheckSuccessEvent)
            } else {
                _event.emit(NfcError(result.exceptionOrNull()))
            }
        }
    }

    fun updateExistingKey(existingKey: WalletsExistingKey, replace: Boolean) {
        viewModelScope.launch {
            _event.emit(NfcLoading)
            updateExistingKeyUseCase(UpdateExistingKeyUseCase.Params(serverSigner = existingKey.signerServer, existingKey.localSigner, replace))
                .onSuccess {
                    _event.emit(SignerInfoEvent.DeleteExistingSignerSuccess(existingKey.localSigner.name))
                }.onFailure {
                    _event.emit(SignerInfoEvent.Error(it))
                }
        }
    }
}