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

package com.nunchuk.android.wallet.components.cosigning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesUpdateKeyPolicyUseCase
import com.nunchuk.android.core.domain.membership.GetKeyPolicyUserDataUseCase
import com.nunchuk.android.core.domain.membership.ParseUpdateKeyPayloadUseCase
import com.nunchuk.android.core.domain.membership.UpdateServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.membership.GetServerKeysUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CosigningPolicyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getServerKeysUseCase: GetServerKeysUseCase,
    private val updateServerKeysUseCase: UpdateServerKeysUseCase,
    private val calculateRequiredSignaturesUpdateKeyPolicyUseCase: CalculateRequiredSignaturesUpdateKeyPolicyUseCase,
    private val getKeyPolicyUserDataUseCase: GetKeyPolicyUserDataUseCase,
    private val getDummyTransactionPayloadUseCase: GetDummyTransactionPayloadUseCase,
    private val parseUpdateKeyPayloadUseCase: ParseUpdateKeyPayloadUseCase,
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
) : ViewModel() {
    private val args: CosigningPolicyFragmentArgs =
        CosigningPolicyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CosigningPolicyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(
        CosigningPolicyState(keyPolicy = args.keyPolicy ?: KeyPolicy())
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (args.dummyTransactionId.isNotEmpty()) {
                getDummyTransactionPayloadUseCase(
                    GetDummyTransactionPayloadUseCase.Param(
                        transactionId = args.dummyTransactionId,
                        walletId = args.walletId
                    )
                ).onSuccess { payload ->
                    if (payload.type == DummyTransactionType.UPDATE_SERVER_KEY) {
                        parseUpdateKeyPayloadUseCase(payload).onSuccess { newPolicy ->
                            _state.update {
                                it.copy(
                                    keyPolicy = newPolicy,
                                    dummyTransactionId = args.dummyTransactionId,
                                    isUpdateFlow = true,
                                    requiredSignature = CalculateRequiredSignatures(
                                        VerificationType.SIGN_DUMMY_TX,
                                        payload.requiredSignatures
                                    ),
                                )
                            }
                        }
                    }
                }.onFailure {
                    Timber.e(it)
                }
                getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                    _state.update { it.copy(walletName = wallet.name) }
                }
            } else {
                getKeyPolicy()
            }
        }
    }

    private suspend fun getKeyPolicy() {
        args.signer?.let { signer ->
            val result = getServerKeysUseCase(
                GetServerKeysUseCase.Param(
                    signer.fingerPrint,
                    signer.derivationPath
                )
            )
            if (result.isSuccess) {
                _state.update { it.copy(originalKeyPolicy = result.getOrThrow()) }
                updateState(keyPolicy = result.getOrThrow())
            }
        }
    }

    fun updateState(keyPolicy: KeyPolicy?, isEditMode: Boolean = false) {
        keyPolicy ?: return
        _state.update {
            it.copy(
                keyPolicy = keyPolicy,
                isUpdateFlow = isEditMode
            )
        }
    }

    fun updateServerConfig(
        signatures: Map<String, String> = emptyMap(),
        securityQuestionToken: String = "",
    ) {
        viewModelScope.launch {
            val signer = args.signer ?: return@launch
            _event.emit(CosigningPolicyEvent.Loading(true))
            val result = updateServerKeysUseCase(
                UpdateServerKeysUseCase.Param(
                    body = state.value.userData,
                    keyIdOrXfp = signer.fingerPrint,
                    signatures = signatures,
                    securityQuestionToken = securityQuestionToken,
                    token = args.token,
                    derivationPath = signer.derivationPath
                )
            )
            _event.emit(CosigningPolicyEvent.Loading(false))
            if (result.isSuccess) {
                getKeyPolicy()
                _event.emit(CosigningPolicyEvent.UpdateKeyPolicySuccess)
                _state.update { it.copy(isUpdateFlow = false) }
            } else {
                _event.emit(CosigningPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onDiscardChangeClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnDiscardChange)
        }
    }

    fun cancelChange() {
        viewModelScope.launch {
            if (args.dummyTransactionId.isEmpty()) {
                _event.emit(CosigningPolicyEvent.CancelChangeSuccess)
                return@launch
            }
            _event.emit(CosigningPolicyEvent.Loading(true))
            deleteGroupDummyTransactionUseCase(
                DeleteGroupDummyTransactionUseCase.Param(
                    walletId = args.walletId,
                    transactionId = args.dummyTransactionId
                )
            ).onSuccess { _event.emit(CosigningPolicyEvent.CancelChangeSuccess) }
                .onFailure { _event.emit(CosigningPolicyEvent.ShowError(it.message.orUnknownError())) }
        }
    }

    fun onSaveChangeClicked() {
        viewModelScope.launch {
            if (state.value.dummyTransactionId.isNotEmpty()) {
                _event.emit(
                    CosigningPolicyEvent.OnSaveChange(
                        _state.value.requiredSignature,
                        _state.value.userData,
                        _state.value.dummyTransactionId
                    )
                )
                return@launch
            }
            val signer = args.signer ?: return@launch
            _event.emit(CosigningPolicyEvent.Loading(true))
            val result = calculateRequiredSignaturesUpdateKeyPolicyUseCase(
                CalculateRequiredSignaturesUpdateKeyPolicyUseCase.Param(
                    walletId = args.walletId,
                    keyPolicy = state.value.keyPolicy,
                    xfp = signer.fingerPrint,
                    derivationPath = signer.derivationPath
                )
            )
            _event.emit(CosigningPolicyEvent.Loading(false))
            if (result.isSuccess) {
                val requiredSignature = result.getOrThrow()
                val data = getKeyPolicyUserDataUseCase(
                    GetKeyPolicyUserDataUseCase.Param(
                        args.walletId,
                        state.value.keyPolicy
                    )
                ).getOrThrow()
                _state.update { it.copy(userData = data) }
                if (requiredSignature.type == VerificationType.SIGN_DUMMY_TX) {
                    updateServerKeysUseCase(
                        UpdateServerKeysUseCase.Param(
                            body = state.value.userData,
                            keyIdOrXfp = signer.fingerPrint,
                            signatures = emptyMap(),
                            securityQuestionToken = "",
                            token = args.token,
                            derivationPath = signer.derivationPath
                        )
                    ).onSuccess { transactionId ->
                        _state.update {
                            it.copy(
                                dummyTransactionId = transactionId,
                                requiredSignature = requiredSignature
                            )
                        }
                        _event.emit(
                            CosigningPolicyEvent.OnSaveChange(
                                required = requiredSignature,
                                data = data,
                                dummyTransactionId = transactionId
                            )
                        )
                    }.onFailure { exception ->
                        _event.emit(CosigningPolicyEvent.ShowError(exception.message.orUnknownError()))
                    }
                } else {
                    _event.emit(
                        CosigningPolicyEvent.OnSaveChange(
                            requiredSignature,
                            data,
                            ""
                        )
                    )
                }
            } else {
                _event.emit(CosigningPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onEditSpendingLimitClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnEditSpendingLimitClicked)
        }
    }

    fun onEditSigningDelayClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnEditSingingDelayClicked)
        }
    }
}

data class CosigningPolicyState(
    val walletName: String = "",
    val originalKeyPolicy: KeyPolicy = KeyPolicy(),
    val keyPolicy: KeyPolicy = KeyPolicy(),
    val isUpdateFlow: Boolean = false,
    val userData: String = "",
    val signingDelayText: String = "",
    val dummyTransactionId: String = "",
    val requiredSignature: CalculateRequiredSignatures = CalculateRequiredSignatures(),
)

sealed class CosigningPolicyEvent {
    class Loading(val isLoading: Boolean) : CosigningPolicyEvent()
    class ShowError(val error: String) : CosigningPolicyEvent()
    class OnSaveChange(
        val required: CalculateRequiredSignatures,
        val data: String,
        val dummyTransactionId: String,
    ) :
        CosigningPolicyEvent()

    data object OnEditSpendingLimitClicked : CosigningPolicyEvent()
    data object OnEditSingingDelayClicked : CosigningPolicyEvent()
    data object OnDiscardChange : CosigningPolicyEvent()
    data object UpdateKeyPolicySuccess : CosigningPolicyEvent()
    data object CancelChangeSuccess : CosigningPolicyEvent()
}