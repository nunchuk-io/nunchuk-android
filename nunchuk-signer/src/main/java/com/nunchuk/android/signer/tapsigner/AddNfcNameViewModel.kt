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

package com.nunchuk.android.signer.tapsigner

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.domain.CreateTapSignerUseCase
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerUseCase
import com.nunchuk.android.core.helper.CheckAssistedSignerExistenceHelper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.UpdateMasterSignerUseCase
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNfcNameViewModel @Inject constructor(
    private val createTapSignerUseCase: CreateTapSignerUseCase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase,
    private val checkAssistedSignerExistenceHelper: CheckAssistedSignerExistenceHelper,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getSignerFromTapsignerMasterSignerUseCase: GetSignerFromTapsignerMasterSignerUseCase,
    private val pushEventManager: PushEventManager,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val addSignerToGroupUseCase: AddSignerToGroupUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = AddNfcNameFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<AddNfcNameEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AddNfcNameState())

    init {
        checkAssistedSignerExistenceHelper.init(viewModelScope)
    }

    fun addNameForNfcKey(
        isoDep: IsoDep?,
        cvc: String,
        name: String,
        shouldCreateBackUp: Boolean = false,
        index: Int,
        walletId: String,
        groupId: String,
        requestedSignerIndex: Int
    ) {
        isoDep ?: return
        viewModelScope.launch {
            _event.emit(AddNfcNameEvent.Loading(true))
            _state.value.signer?.takeIf { shouldCreateBackUp }?.let {
                getBackUpTapSigner(isoDep, cvc, it)
                _event.emit(AddNfcNameEvent.Loading(false))
                return@launch
            }
            createTapSignerUseCase(
                CreateTapSignerUseCase.Data(
                    isoDep,
                    cvc,
                    name,
                    _state.value.replace
                )
            ).onSuccess { signer ->
                _state.update { it.copy(signer = signer) }

                if (shouldCreateBackUp) {
                    getBackUpTapSigner(isoDep, cvc, signer)
                } else {
                    // for replace key free wallet
                    if (walletId.isNotEmpty()) {
                        loadSingleSigner(index, isoDep, cvc, signer, walletId)
                    } else if (index >= 0 && groupId.isNotEmpty()) {
                        addKeyToFreeGroup(isoDep, cvc, signer, groupId, index, requestedSignerIndex)
                    }
                    _event.emit(AddNfcNameEvent.Success(signer))
                }
            }.onFailure {
                val errorCode = it.nativeErrorCode()
                if (errorCode == NativeErrorCode.SIGNER_EXISTS) {
                    _event.emit(
                        AddNfcNameEvent.SignerExist(cardIdent = args.cardIdent)
                    )
                } else {
                    _event.emit(AddNfcNameEvent.Error(it))
                }
            }
            _event.emit(AddNfcNameEvent.Loading(false))
        }
    }

    private suspend fun addKeyToFreeGroup(
        isoDep: IsoDep,
        cvc: String,
        signer: MasterSigner,
        groupId: String,
        index: Int,
        requestedSignerIndex: Int
    ) {
        if (index > 0) {
            getSignerFromTapsignerMasterSignerUseCase(
                GetSignerFromTapsignerMasterSignerUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    masterSignerId = signer.id,
                    index = index,
                    walletType = WalletType.MULTI_SIG
                )
            )
        }
        getSignerFromMasterSignerUseCase(
            GetSignerFromMasterSignerUseCase.Param(
                xfp = signer.id,
                walletType = WalletType.MULTI_SIG,
                addressType = AddressType.NATIVE_SEGWIT,
                index = index
            )
        ).map {
            if (it != null) {
                addSignerToGroupUseCase(
                    AddSignerToGroupUseCase.Params(
                        groupId = groupId,
                        signer = it,
                        index = requestedSignerIndex
                    )
                )
            }
        }
    }

    private suspend fun loadSingleSigner(
        index: Int,
        isoDep: IsoDep,
        cvc: String,
        signer: MasterSigner,
        walletId: String
    ) {
        getWalletDetail2UseCase(walletId).onSuccess { wallet ->
            val walletType =
                if (wallet.signers.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
            if (index > 0) {
                getSignerFromTapsignerMasterSignerUseCase(
                    GetSignerFromTapsignerMasterSignerUseCase.Data(
                        isoDep = isoDep,
                        cvc = cvc,
                        masterSignerId = signer.id,
                        index = index,
                        walletType = walletType
                    )
                )
            }

            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Param(
                    xfp = signer.id,
                    walletType = walletType,
                    addressType = AddressType.NATIVE_SEGWIT,
                    index = index
                )
            ).onSuccess { singleSigner ->
                singleSigner?.let {
                    pushEventManager.push(PushEvent.LocalUserSignerAdded(it))
                }
            }
        }
    }

    fun setReplaceKeyFlow(replace: Boolean) {
        _state.update { it.copy(replace = replace) }
    }

    private suspend fun getBackUpTapSigner(
        isoDep: IsoDep, cvc: String, signer: MasterSigner
    ) {
        if (_state.value.filePath.isNotEmpty()) {
            _event.emit(AddNfcNameEvent.BackUpSuccess(_state.value.filePath))
            return
        }
        val createBackUpKeyResult =
            getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc, signer.id))
        if (createBackUpKeyResult.isSuccess) {
            _event.emit(AddNfcNameEvent.BackUpSuccess(createBackUpKeyResult.getOrThrow()))
            _state.update { it.copy(filePath = createBackUpKeyResult.getOrThrow()) }
        } else {
            _event.emit(AddNfcNameEvent.Error(createBackUpKeyResult.exceptionOrNull()))
        }
    }

    fun updateName(masterSigner: MasterSigner, updateSignerName: String) {
        viewModelScope.launch {
            updateMasterSignerUseCase(parameters = masterSigner.copy(name = updateSignerName))
                .onSuccess {
                    _event.emit(
                        AddNfcNameEvent.Success(masterSigner.copy(name = updateSignerName))
                    )
                }.onFailure { e ->
                    _event.emit(AddNfcNameEvent.UpdateError(e))
                }
        }
    }

    fun getMasterSigner(): MasterSigner? = _state.value.signer
}

data class AddNfcNameState(
    val signer: MasterSigner? = null,
    val filePath: String = "",
    val replace: Boolean = false
)

sealed class AddNfcNameEvent {
    data class Loading(val isLoading: Boolean) : AddNfcNameEvent()
    data class Success(val masterSigner: MasterSigner) : AddNfcNameEvent()
    data class BackUpSuccess(val filePath: String) : AddNfcNameEvent()
    data class Error(val e: Throwable?) : AddNfcNameEvent()
    data class UpdateError(val e: Throwable?) : AddNfcNameEvent()
    data class SignerExist(val cardIdent: String) : AddNfcNameEvent()
}