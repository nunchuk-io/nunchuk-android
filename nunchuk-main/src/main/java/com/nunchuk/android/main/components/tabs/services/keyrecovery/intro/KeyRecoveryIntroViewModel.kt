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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesKeyRecoveryUseCase
import com.nunchuk.android.core.domain.membership.DownloadBackupKeyUseCase
import com.nunchuk.android.core.domain.membership.RecoverKeyUseCase
import com.nunchuk.android.core.domain.membership.RequestRecoverUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryIntroViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val cardIdManager: CardIdManager,
    private val downloadBackupKeyUseCase: DownloadBackupKeyUseCase,
    private val calculateRequiredSignaturesKeyRecoveryUseCase: CalculateRequiredSignaturesKeyRecoveryUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val requestRecoverUseCase: RequestRecoverUseCase,
    private val recoverKeyUseCase: RecoverKeyUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args =
        KeyRecoveryIntroFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(KeyRecoveryIntroState())

    private val _event = MutableSharedFlow<KeyRecoveryIntroEvent>()
    val event = _event.asSharedFlow()

    var isHasGroup = false

    init {
        viewModelScope.launch {
            getGroupsUseCase(Unit).distinctUntilChanged().collect {
                isHasGroup = it.getOrNull().isNullOrEmpty().not()
            }
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { assistedWallets ->
                    _state.update {
                        it.copy(assistedWallets = assistedWallets.map { it.localId }.toHashSet())
                    }
                }
        }
        viewModelScope.launch {
            getMasterSignersUseCase.execute().zip(getWalletsUseCase.execute()) { p, wallets ->
                Pair(p, wallets)
            }.map {
                val (masterSigners, wallets) = it
                val signers = masterSigners
                    .filter { it.device.isTapsigner }
                    .map { signer ->
                        masterSignerMapper(signer)
                    }.map {
                        it.copy(cardId = cardIdManager.getCardId(signerId = it.id))
                    }.toList()
                return@map Pair(signers, wallets)
            }.collect { (tapSigners, wallets) ->
                _state.update {
                    it.copy(
                        tapSigners = tapSigners,
                        wallets = wallets
                    )
                }
            }
        }
    }

    fun getTapSignerList() = viewModelScope.launch {
        _event.emit(KeyRecoveryIntroEvent.Loading(true))
        val assistedWallets = _state.value.wallets.filter { _state.value.assistedWallets.contains(it.wallet.id) }
        val signers = _state.value.tapSigners.filter {
            isInWallet(assistedWallets, it)
        }
        _event.emit(KeyRecoveryIntroEvent.Loading(false))
        _event.emit(KeyRecoveryIntroEvent.GetTapSignerSuccess(signers))
    }

    private fun isInWallet(assistedWallets: List<WalletExtended>, signer: SignerModel): Boolean {
        return assistedWallets.any {
            it.wallet.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                }
                return@anyLast singleSigner.masterFingerprint == signer.fingerPrint && singleSigner.derivationPath == signer.derivationPath
            }
        }
    }

    fun setSelectedSigner(signer: SignerModel) = viewModelScope.launch {
        _state.update {
            it.copy(selectedSigner = signer)
        }
    }

    fun downloadBackupKey(questionId: String, answer: String) = viewModelScope.launch {
        val state = _state.value
        if (state.selectedSigner == null) {
            return@launch
        }
        _event.emit(KeyRecoveryIntroEvent.Loading(true))
        val result = downloadBackupKeyUseCase(
            DownloadBackupKeyUseCase.Param(
                id = state.selectedSigner.fingerPrint,
                questionId = questionId,
                answer = answer,
                verifyToken = args.verifyToken
            )
        )
        _event.emit(KeyRecoveryIntroEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(
                KeyRecoveryIntroEvent.DownloadBackupKeySuccess(result.getOrThrow())
            )
        } else {
            _event.emit(KeyRecoveryIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun calculateRequiredSignatures() = viewModelScope.launch {
        val state = _state.value
        if (state.selectedSigner == null) {
            return@launch
        }
        _event.emit(KeyRecoveryIntroEvent.Loading(true))
        val result = calculateRequiredSignaturesKeyRecoveryUseCase(
            CalculateRequiredSignaturesKeyRecoveryUseCase.Param(
                xfp = state.selectedSigner.fingerPrint
            )
        )
        _event.emit(KeyRecoveryIntroEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(
                KeyRecoveryIntroEvent.CalculateRequiredSignaturesSuccess(result.getOrThrow())
            )
        } else {
            _event.emit(KeyRecoveryIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun requestRecover(
        signatures: HashMap<String, String>,
        securityQuestionToken: String, confirmCodeToken: String, confirmCodeNonce: String
    ) {
        viewModelScope.launch {
            val state = _state.value
            if (state.selectedSigner == null) {
                return@launch
            }
            _event.emit(KeyRecoveryIntroEvent.Loading(true))
            val result = requestRecoverUseCase(
                RequestRecoverUseCase.Param(
                    signatures = signatures,
                    verifyToken = args.verifyToken,
                    securityQuestionToken = securityQuestionToken,
                    confirmCodeToken = confirmCodeToken,
                    confirmCodeNonce = confirmCodeNonce,
                    xfp = state.selectedSigner.fingerPrint
                )
            )
            _event.emit(KeyRecoveryIntroEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(KeyRecoveryIntroEvent.RequestRecoverSuccess)
            } else {
                _event.emit(KeyRecoveryIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun recoverKey() {
        viewModelScope.launch {
            val state = _state.value
            if (state.selectedSigner == null) {
                return@launch
            }
            _event.emit(KeyRecoveryIntroEvent.Loading(true))
            val result = recoverKeyUseCase(
                RecoverKeyUseCase.Param(
                    xfp = state.selectedSigner.fingerPrint
                )
            )
            _event.emit(KeyRecoveryIntroEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(KeyRecoveryIntroEvent.DownloadBackupKeySuccess(result.getOrThrow()))
            } else {
                _event.emit(KeyRecoveryIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}