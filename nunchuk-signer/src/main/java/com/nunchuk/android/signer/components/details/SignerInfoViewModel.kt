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
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GenerateColdCardHealthCheckMessageUseCase
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.HealthCheckColdCardUseCase
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.domain.HealthCheckTapSignerUseCase
import com.nunchuk.android.core.domain.TopUpXpubTapSignerUseCase
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.isRemoteSigner
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    savedStateHandle: SavedStateHandle,
    getAssistedKeysUseCase: GetAssistedKeysUseCase,
) : NunchukViewModel<SignerInfoState, SignerInfoEvent>() {

    override val initialState = SignerInfoState()

    private val assistedKeys = getAssistedKeysUseCase(Unit)
        .map { it.getOrDefault(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val args: SignerInfoFragmentArgs =
        SignerInfoFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            if (shouldLoadMasterSigner(args.signerType)) {
                val result = getMasterSignerUseCase(args.id)
                if (result.isSuccess) {

                    updateState { copy(masterSigner = result.getOrThrow()) }
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
                    updateState { copy(remoteSigner = result.getOrThrow()) }
                }
            }
        }
        if (args.signerType == SignerType.NFC) {
            viewModelScope.launch {
                val cardId = cardIdManager.getCardId(args.id)
                updateState { copy(nfcCardId = cardId) }
            }
        }
    }

    fun handleEditCompletedEvent(updateSignerName: String) {
        viewModelScope.launch {
            val state = getState()
            if (shouldLoadMasterSigner(args.signerType)) {
                state.masterSigner?.let { signer ->
                    updateMasterSignerUseCase(parameters = signer.copy(name = updateSignerName))
                        .onSuccess {
                            event(UpdateNameSuccessEvent(updateSignerName))
                            updateServerKeyName(signer.id, updateSignerName)
                        }
                        .onFailure { e ->
                            event(UpdateNameErrorEvent(e.message.orUnknownError()))
                        }
                }
            } else {
                state.remoteSigner?.let { signer ->
                    when (val result =
                        updateRemoteSignerUseCase.execute(signer = signer.copy(name = updateSignerName))) {
                        is Success -> {
                            event(UpdateNameSuccessEvent(updateSignerName))
                            updateServerKeyName(signer.masterFingerprint, updateSignerName)
                        }

                        is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
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
            if (shouldLoadMasterSigner(args.signerType)) {
                state.masterSigner?.let {
                    deleteMasterSignerUseCase(it.id)
                        .onSuccess {
                            event(RemoveSignerCompletedEvent)
                        }.onFailure { exception ->
                            event(RemoveSignerErrorEvent(exception.message.orUnknownError()))
                        }
                }
            } else {
                state.remoteSigner?.let {
                    deleteRemoteSignerUseCase(DeleteRemoteSignerUseCase.Params(
                        masterFingerprint = it.masterFingerprint,
                        derivationPath = it.derivationPath
                    )).onSuccess {
                        event(RemoveSignerCompletedEvent)
                    }.onFailure { exception ->
                        event(RemoveSignerErrorEvent(exception.message.orUnknownError()))
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
                    .onException { event(HealthCheckErrorEvent(it.message.orEmpty())) }
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
                .onException { event(HealthCheckErrorEvent(it.message)) }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (it == HealthStatus.SUCCESS) {
                        event(HealthCheckSuccessEvent)
                    } else {
                        event(HealthCheckErrorEvent())
                    }
                }
        }
    }

    fun healthCheckTapSigner(isoDep: IsoDep, cvc: String, masterSigner: MasterSigner) {
        viewModelScope.launch {
            event(NfcLoading)
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
                event(HealthCheckSuccessEvent)
            } else {
                event(HealthCheckErrorEvent(e = result.exceptionOrNull()))
            }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, cvc: String) {
        val masterSignerId = state.value?.masterSigner?.id ?: return
        viewModelScope.launch {
            event(NfcLoading)
            val result = getTapSignerBackupUseCase(
                GetTapSignerBackupUseCase.Data(
                    isoDep,
                    cvc,
                    masterSignerId
                )
            )
            if (result.isSuccess) {
                event(GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                event(NfcError(result.exceptionOrNull()))
            }
        }
    }

    fun topUpXpubTapSigner(isoDep: IsoDep, cvc: String, masterSignerId: String) {
        viewModelScope.launch {
            event(NfcLoading)
            val result = topUpXpubTapSignerUseCase(
                TopUpXpubTapSignerUseCase.Data(
                    isoDep,
                    cvc,
                    masterSignerId
                )
            )
            if (result.isSuccess) {
                event(TopUpXpubSuccess)
            } else {
                event(TopUpXpubFailed(result.exceptionOrNull()))
            }
        }
    }

    private fun shouldLoadMasterSigner(type: SignerType) = !type.isRemoteSigner

    fun generateColdcardHealthMessages(ndef: Ndef?, derivationPath: String) {
        ndef ?: return
        viewModelScope.launch {
            event(NfcLoading)
            val result = generateColdCardHealthCheckMessageUseCase(
                GenerateColdCardHealthCheckMessageUseCase.Data(
                    derivationPath = derivationPath,
                    ndef = ndef
                )
            )
            if (result.isSuccess) {
                setEvent(GenerateColdcardHealthMessagesSuccess)
            } else {
                setEvent(NfcError(result.exceptionOrNull()))
            }
        }
    }

    fun healthCheckColdCard(signer: SingleSigner, records: List<NdefRecord>) {
        viewModelScope.launch {
            event(NfcLoading)
            val result =
                healthCheckColdCardUseCase(HealthCheckColdCardUseCase.Param(signer, records))
            if (result.isSuccess) {
                setEvent(HealthCheckSuccessEvent)
            } else {
                setEvent(NfcError(result.exceptionOrNull()))
            }
        }
    }
}