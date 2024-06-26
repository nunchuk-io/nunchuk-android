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

package com.nunchuk.android.signer.tapsigner.backup.upload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.KeyUpload
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.UploadBackupFileKeyUseCase
import com.nunchuk.android.usecase.replace.UploadReplaceBackupFileKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadBackUpTapSignerViewModel @Inject constructor(
    private val uploadBackupFileKeyUseCase: UploadBackupFileKeyUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val uploadReplaceBackupFileKeyUseCase: UploadReplaceBackupFileKeyUseCase,
    private val cardIdManager: CardIdManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = UploadBackUpTapSignerFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<UploadBackUpTapSignerEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(UploadBackUpTapSignerState())
    val state = _state.asStateFlow()

    private var isAddNewKey = true
    private var groupId = ""
    private var signerIndex = 0
    private var replacedXfp = ""
    private var walletId = ""

    fun init(
        isAddNewKey: Boolean,
        groupId: String,
        signerIndex: Int,
        replacedXfp: String,
        walletId: String
    ) {
        this.isAddNewKey = isAddNewKey
        this.groupId = groupId
        this.signerIndex = signerIndex
        this.replacedXfp = replacedXfp
        this.walletId = walletId
    }

    fun upload() {
        viewModelScope.launch {
            val result = getMasterSignerUseCase(args.masterSignerId)
            if (replacedXfp.isNotEmpty() && walletId.isNotEmpty()) {
                uploadReplaceBackupFileKeyUseCase(
                    UploadReplaceBackupFileKeyUseCase.Param(
                        keyName = result.getOrNull()?.name.orEmpty(),
                        keyType = SignerType.NFC.name,
                        xfp = args.masterSignerId,
                        cardId = cardIdManager.getCardId(args.masterSignerId),
                        filePath = args.filePath,
                        isAddNewKey = isAddNewKey,
                        signerIndex = signerIndex,
                        groupId = groupId,
                        replacedXfp = replacedXfp,
                        walletId = walletId
                    )
                )
            } else {
                uploadBackupFileKeyUseCase(
                    UploadBackupFileKeyUseCase.Param(
                        step = membershipStepManager.currentStep ?: MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                        keyName = result.getOrNull()?.name.orEmpty(),
                        keyType = SignerType.NFC.name,
                        xfp = args.masterSignerId,
                        cardId = cardIdManager.getCardId(args.masterSignerId),
                        filePath = args.filePath,
                        isAddNewKey = isAddNewKey,
                        plan = membershipStepManager.localMembershipPlan.takeIf { groupId.isEmpty() } ?: MembershipPlan.BYZANTINE,
                        groupId = groupId,
                        signerIndex = signerIndex,
                    )
                )
            }.collect {
                if (it.isSuccess) {
                    when (val content = it.getOrThrow()) {
                        is KeyUpload.Progress -> {
                            _state.update { state ->
                                state.copy(
                                    percent = content.value,
                                    isError = false
                                )
                            }
                        }
                        is KeyUpload.Data -> {
                            _state.update { state ->
                                state.copy(serverFilePath = content.filePath)
                            }
                        }
                        is KeyUpload.KeyVerified -> {
                            _event.emit(UploadBackUpTapSignerEvent.KeyVerified(content.message))
                        }
                    }
                } else {
                    _state.update { state -> state.copy(isError = true) }
                    _event.emit(UploadBackUpTapSignerEvent.ShowError(it.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }

    fun getServerFilePath(): String = state.value.serverFilePath

    fun onContinueClicked() {
        viewModelScope.launch {
            if (_state.value.isError) {
                upload()
                _state.update { state -> state.copy(percent = 0, isError = false) }
            } else {
                _event.emit(UploadBackUpTapSignerEvent.OnContinueClicked)
            }
        }
    }
}

data class UploadBackUpTapSignerState(
    val percent: Int = 0,
    val isError: Boolean = false,
    val serverFilePath: String = ""
)

sealed class UploadBackUpTapSignerEvent {
    object OnContinueClicked : UploadBackUpTapSignerEvent()
    data class KeyVerified(val message: String) : UploadBackUpTapSignerEvent()
    data class ShowError(val message: String) : UploadBackUpTapSignerEvent()
}