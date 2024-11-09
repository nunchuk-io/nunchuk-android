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

package com.nunchuk.android.signer.components.backup

import android.util.Log
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
class BackingUpViewModel @Inject constructor(
    private val uploadBackupFileKeyUseCase: UploadBackupFileKeyUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val uploadReplaceBackupFileKeyUseCase: UploadReplaceBackupFileKeyUseCase,
    private val cardIdManager: CardIdManager,
) : ViewModel() {
    private val _event = MutableSharedFlow<BackingUpEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(BackingUpState())
    val state = _state.asStateFlow()

    private var isAddNewKey = true
    private var groupId = ""
    private var signerIndex = 0
    private var replacedXfp = ""
    private var walletId = ""
    private var filePath = ""
    private var masterSignerId = ""
    private var signerType = SignerType.NFC
    private var keyName = ""
    private var isRequestAddOrReplaceKey = true

    fun init(
        isAddNewKey: Boolean,
        groupId: String,
        signerIndex: Int,
        replacedXfp: String,
        walletId: String,
        filePath: String,
        masterSignerId: String,
        signerType: SignerType = SignerType.NFC,
        keyName: String = "",
        isRequestAddOrReplaceKey: Boolean
    ) {
        this.isAddNewKey = isAddNewKey
        this.groupId = groupId
        this.signerIndex = signerIndex
        this.replacedXfp = replacedXfp
        this.walletId = walletId
        this.filePath = filePath
        this.masterSignerId = masterSignerId
        this.signerType = signerType
        this.keyName = keyName
        this.isRequestAddOrReplaceKey = isRequestAddOrReplaceKey
    }

    fun upload() {
        viewModelScope.launch {
            if (signerType == SignerType.NFC) {
                val result = getMasterSignerUseCase(masterSignerId)
                keyName = result.getOrNull()?.name.orEmpty()
            }
            if (replacedXfp.isNotEmpty() && walletId.isNotEmpty()) {
                uploadReplaceBackupFileKeyUseCase(
                    UploadReplaceBackupFileKeyUseCase.Param(
                        keyName = keyName,
                        keyType = signerType.name,
                        xfp = masterSignerId,
                        cardId = cardIdManager.getCardId(masterSignerId),
                        filePath = filePath,
                        isAddNewKey = isAddNewKey,
                        signerIndex = signerIndex,
                        groupId = groupId,
                        replacedXfp = replacedXfp,
                        walletId = walletId,
                        isRequestReplaceKey = isRequestAddOrReplaceKey
                    )
                )
            } else {
                uploadBackupFileKeyUseCase(
                    UploadBackupFileKeyUseCase.Param(
                        step = membershipStepManager.currentStep
                            ?: MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                        keyName = keyName,
                        keyType = signerType.name,
                        xfp = masterSignerId,
                        cardId = cardIdManager.getCardId(masterSignerId),
                        filePath = filePath,
                        isAddNewKey = isAddNewKey,
                        plan = membershipStepManager.localMembershipPlan.takeIf { groupId.isEmpty() }
                            ?: MembershipPlan.BYZANTINE,
                        groupId = groupId,
                        signerIndex = signerIndex,
                        isRequestAddKey = isRequestAddOrReplaceKey
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
                                state.copy(serverFilePath = content.filePath, backUpFileName = content.backUpFileName, keyId = content.keyId)
                            }
                        }

                        is KeyUpload.KeyVerified -> {
                            _event.emit(BackingUpEvent.KeyVerified(content.message))
                        }
                    }
                } else {
                    _state.update { state -> state.copy(isError = true) }
                    _event.emit(BackingUpEvent.ShowError(it.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }

    fun getServerFilePath(): String = state.value.serverFilePath
    fun getBackUpFileName(): String = state.value.backUpFileName
    fun getKeyId(): String = state.value.keyId

    fun onContinueClicked() {
        viewModelScope.launch {
            if (_state.value.isError) {
                upload()
                _state.update { state -> state.copy(percent = 0, isError = false) }
            } else {
                _event.emit(BackingUpEvent.OnContinueClicked)
            }
        }
    }
}

data class BackingUpState(
    val percent: Int = 0,
    val isError: Boolean = false,
    val serverFilePath: String = "",
    val backUpFileName: String = "",
    val keyId: String = ""
)

sealed class BackingUpEvent {
    data object OnContinueClicked : BackingUpEvent()
    data class KeyVerified(val message: String) : BackingUpEvent()
    data class ShowError(val message: String) : BackingUpEvent()
}