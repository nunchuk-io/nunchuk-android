/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.byzantine.ParseUpdateGroupKeyPayloadUseCase
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase
import com.nunchuk.android.core.domain.membership.GetGroupKeyPolicyUserDataUseCase
import com.nunchuk.android.core.domain.membership.UpdateGroupServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.isKeyHolder
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.usecase.byzantine.DeleteGroupDummyTransactionUseCase
import com.nunchuk.android.usecase.byzantine.GetDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupRemoteUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupServerKeysUseCase
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
class CosigningGroupPolicyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupServerKeysUseCase: GetGroupServerKeysUseCase,
    private val updateGroupServerKeysUseCase: UpdateGroupServerKeysUseCase,
    private val calculateRequiredSignaturesUpdateGroupKeyPolicyUseCase: CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase,
    private val getGroupKeyPolicyUserDataUseCase: GetGroupKeyPolicyUserDataUseCase,
    private val getGroupRemoteUseCase: GetGroupRemoteUseCase,
    private val getDummyTransactionPayloadUseCase: GetDummyTransactionPayloadUseCase,
    private val parseUpdateGroupKeyPayloadUseCase: ParseUpdateGroupKeyPayloadUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val deleteGroupDummyTransactionUseCase: DeleteGroupDummyTransactionUseCase,
    private val accountManager: AccountManager
) : ViewModel() {
    private val args: CosigningGroupPolicyFragmentArgs =
        CosigningGroupPolicyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CosigningGroupPolicyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CosigningGroupPolicyState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            loadMembers()
            if (args.dummyTransactionId.isNotEmpty()) {
                getDummyTransactionPayloadUseCase(
                    GetDummyTransactionPayloadUseCase.Param(
                        groupId = args.groupId,
                        transactionId = args.dummyTransactionId,
                        walletId = args.walletId
                    )
                ).onSuccess { payload ->
                    if (payload.type == DummyTransactionType.UPDATE_SERVER_KEY) {
                        parseUpdateGroupKeyPayloadUseCase(payload).onSuccess { newPolicy ->
                            _state.update {
                                it.copy(
                                    keyPolicy = newPolicy,
                                    dummyTransactionId = args.dummyTransactionId,
                                    isUpdateFlow = true,
                                    requiredSignature = CalculateRequiredSignatures(
                                        VerificationType.SIGN_DUMMY_TX,
                                        payload.requiredSignatures
                                    ),
                                    requestByUserId = payload.requestByUserId,
                                    pendingSignature = payload.pendingSignatures
                                )
                            }
                        }

                        getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                            _state.update { it.copy(walletName = wallet.name) }
                        }
                    }
                }
            } else {
                getKeyPolicy()
            }
            _event.emit(CosigningGroupPolicyEvent.Loading(false))
        }
    }

    private suspend fun getKeyPolicy() {
        args.signer?.let { signer ->
            getGroupServerKeysUseCase(
                GetGroupServerKeysUseCase.Param(
                    args.groupId,
                    signer.fingerPrint,
                    signer.derivationPath
                )
            ).onSuccess {
                _state.update { state -> state.copy(keyPolicy = it, originKeyPolicy = it) }
            }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            getGroupRemoteUseCase(GetGroupRemoteUseCase.Params(args.groupId)).onSuccess { group ->
                val myEmail = accountManager.getAccount().email
                val members = group.members.mapNotNull { member ->
                    if (member.role.toRole.isKeyHolder) {
                        AssistedMember(
                            role = member.role,
                            email = member.emailOrUsername,
                            name = member.user?.name,
                            membershipId = member.membershipId,
                            userId = member.user?.id.orEmpty(),
                        )
                    } else null
                }
                _state.update { state ->
                    state.copy(
                        members = members,
                        myRole = members.find { it.email == myEmail }?.role?.toRole
                            ?: AssistedWalletRole.NONE
                    )
                }
            }
        }
    }

    fun updateState(keyPolicy: GroupKeyPolicy?, isEditMode: Boolean) {
        keyPolicy ?: return
        _state.update {
            it.copy(
                keyPolicy = keyPolicy,
                dummyTransactionId = "",
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
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            val result = updateGroupServerKeysUseCase(
                UpdateGroupServerKeysUseCase.Param(
                    body = state.value.userData,
                    keyIdOrXfp = signer.fingerPrint,
                    derivationPath = signer.derivationPath,
                    signatures = signatures,
                    securityQuestionToken = securityQuestionToken,
                    token = args.token,
                    groupId = args.groupId,
                )
            )
            if (result.isSuccess) {
                getKeyPolicy()
                _event.emit(CosigningGroupPolicyEvent.UpdateKeyPolicySuccess)
                _state.update { it.copy(isUpdateFlow = false) }
            } else {
                _event.emit(CosigningGroupPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(CosigningGroupPolicyEvent.Loading(false))
        }
    }

    fun onDiscardChangeClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnDiscardChange(args.dummyTransactionId.isNotEmpty()))
        }
    }

    fun onSaveChangeClicked() {
        viewModelScope.launch {
            if (state.value.dummyTransactionId.isNotEmpty()) {
                _event.emit(
                    CosigningGroupPolicyEvent.OnSaveChange(
                        _state.value.requiredSignature,
                        _state.value.userData,
                        _state.value.dummyTransactionId
                    )
                )
                return@launch
            }
            val signer = args.signer ?: return@launch
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            val result = calculateRequiredSignaturesUpdateGroupKeyPolicyUseCase(
                CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase.Param(
                    walletId = args.walletId,
                    keyPolicy = state.value.keyPolicy,
                    xfp = signer.fingerPrint,
                    derivationPath = signer.derivationPath,
                    groupId = args.groupId
                )
            )
            _event.emit(CosigningGroupPolicyEvent.Loading(false))
            if (result.isSuccess) {
                val requiredSignature = result.getOrThrow()
                getGroupKeyPolicyUserDataUseCase(
                    GetGroupKeyPolicyUserDataUseCase.Param(
                        args.walletId,
                        state.value.keyPolicy
                    )
                ).onSuccess { data ->
                    _state.update { it.copy(userData = data) }
                    if (requiredSignature.type == VerificationType.SIGN_DUMMY_TX) {
                        updateGroupServerKeysUseCase(
                            UpdateGroupServerKeysUseCase.Param(
                                body = data,
                                keyIdOrXfp = signer.fingerPrint,
                                signatures = emptyMap(),
                                securityQuestionToken = "",
                                token = args.token,
                                groupId = args.groupId,
                                derivationPath = signer.derivationPath,
                                draft = true
                            )
                        ).onSuccess { transactionId ->
                            _state.update {
                                it.copy(
                                    dummyTransactionId = transactionId,
                                    requiredSignature = requiredSignature
                                )
                            }
                            _event.emit(
                                CosigningGroupPolicyEvent.OnSaveChange(
                                    required = requiredSignature,
                                    data = data,
                                    dummyTransactionId = transactionId
                                )
                            )
                        }.onFailure { exception ->
                            _event.emit(CosigningGroupPolicyEvent.ShowError(exception.message.orUnknownError()))
                        }
                    } else {
                        _event.emit(
                            CosigningGroupPolicyEvent.OnSaveChange(
                                requiredSignature,
                                data,
                                ""
                            )
                        )
                    }
                }
            } else {
                _event.emit(CosigningGroupPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onEditSpendingLimitClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnEditSpendingLimitClicked)
        }
    }

    fun onEditSigningDelayClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnEditSingingDelayClicked)
        }
    }

    fun cancelChange() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            deleteGroupDummyTransactionUseCase(
                DeleteGroupDummyTransactionUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    transactionId = args.dummyTransactionId
                )
            ).onSuccess { _event.emit(CosigningGroupPolicyEvent.CancelChangeSuccess) }
                .onFailure { _event.emit(CosigningGroupPolicyEvent.ShowError(it.message.orUnknownError())) }
        }
    }
}

data class CosigningGroupPolicyState(
    val keyPolicy: GroupKeyPolicy = GroupKeyPolicy(),
    val originKeyPolicy: GroupKeyPolicy = GroupKeyPolicy(),
    val members: List<AssistedMember> = emptyList(),
    val isUpdateFlow: Boolean = false,
    val userData: String = "",
    val signingDelayText: String = "",
    val dummyTransactionId: String = "",
    val walletName: String = "",
    val requestByUserId: String = "",
    val requiredSignature: CalculateRequiredSignatures = CalculateRequiredSignatures(),
    val pendingSignature: Int = 0,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE
)

sealed class CosigningGroupPolicyEvent {
    data class Loading(val isLoading: Boolean) : CosigningGroupPolicyEvent()
    data class ShowError(val error: String) : CosigningGroupPolicyEvent()
    data class OnSaveChange(
        val required: CalculateRequiredSignatures,
        val data: String,
        val dummyTransactionId: String
    ) : CosigningGroupPolicyEvent()

    object OnEditSpendingLimitClicked : CosigningGroupPolicyEvent()
    object OnEditSingingDelayClicked : CosigningGroupPolicyEvent()
    data class OnDiscardChange(val isCancel: Boolean) : CosigningGroupPolicyEvent()
    object UpdateKeyPolicySuccess : CosigningGroupPolicyEvent()
    object CancelChangeSuccess : CosigningGroupPolicyEvent()
}