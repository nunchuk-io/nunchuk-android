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

package com.nunchuk.android.main.membership.key

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.SIGNER_PATH_PREFIX
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.*
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.ReuseKeyWalletUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyListViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
    private val reuseKeyWalletUseCase: ReuseKeyWalletUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val checkRequestAddDesktopKeyStatusUseCase: CheckRequestAddDesktopKeyStatusUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState = getMembershipStepUseCase(GetMembershipStepUseCase.Param(membershipStepManager.plan, ""))
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        if (membershipStepManager.plan == MembershipPlan.IRON_HAND) {
            _keys.value = listOf(
                AddKeyData(type = MembershipStep.IRON_ADD_HARDWARE_KEY_1),
                AddKeyData(type = MembershipStep.IRON_ADD_HARDWARE_KEY_2),
                AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
            )
        } else {
            _keys.value = listOf(
                AddKeyData(type = MembershipStep.HONEY_ADD_TAP_SIGNER),
                AddKeyData(type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1),
                AddKeyData(type = MembershipStep.HONEY_ADD_HARDWARE_KEY_2),
                AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
            )
        }
        loadSigners()
        viewModelScope.launch {
            membershipStepState.collect {
                val news = _keys.value.map { addKeyData ->
                    val info = getStepInfo(addKeyData.type)
                    if (addKeyData.signer == null && info.masterSignerId.isNotEmpty()) {
                        val extra = runCatching {
                            gson.fromJson(
                                info.extraData,
                                SignerExtra::class.java
                            )
                        }.getOrNull()
                        if (extra?.signerType == SignerType.COLDCARD_NFC
                            || extra?.signerType == SignerType.AIRGAP
                            || extra?.signerType == SignerType.HARDWARE) {
                            val result = getRemoteSignerUseCase(
                                GetRemoteSignerUseCase.Data(
                                    info.masterSignerId,
                                    extra.derivationPath
                                )
                            )
                            if (result.isSuccess) {
                                return@map addKeyData.copy(
                                    signer = result.getOrThrow().toModel(),
                                    verifyType = info.verifyType
                                )
                            }
                        } else {
                            val result = getMasterSignerUseCase(info.masterSignerId)
                            if (result.isSuccess) {
                                return@map addKeyData.copy(
                                    signer = masterSignerMapper(
                                        result.getOrThrow(),
                                    ),
                                    verifyType = info.verifyType
                                )
                            }
                        }
                    }
                    addKeyData.copy(verifyType = info.verifyType)
                }
                _keys.value = news
            }
        }
        viewModelScope.launch {
            checkRequestAddDesktopKeyStatusUseCase(CheckRequestAddDesktopKeyStatusUseCase.Param(membershipStepManager.plan))
        }
    }

    private fun loadSigners() {
        viewModelScope.launch {
            getCompoundSignersUseCase.execute().collect { pair ->
                _state.update {
                    singleSigners.apply {
                        clear()
                        addAll(pair.second)
                    }
                    it.copy(
                        signers = pair.first.map { signer ->
                            masterSignerMapper(signer)
                        } + pair.second.map { signer -> signer.toModel() }
                    )
                }
            }
        }
    }

    // COLDCARD or Airgap
    fun onSelectedExistingHardwareSigner(signer: SignerModel) {
        viewModelScope.launch {
            val hasTag =
                signer.tags.any { it == SignerTag.SEEDSIGNER || it == SignerTag.KEYSTONE || it == SignerTag.PASSPORT || it == SignerTag.JADE }
            if (signer.type == SignerType.COLDCARD_NFC || hasTag || signer.type == SignerType.COLDCARD_NFC) {
                saveMembershipStepUseCase(
                    MembershipStepInfo(
                        step = membershipStepManager.currentStep
                            ?: throw IllegalArgumentException("Current step empty"),
                        masterSignerId = signer.fingerPrint,
                        plan = membershipStepManager.plan,
                        verifyType = VerifyType.APP_VERIFIED,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = signer.derivationPath,
                                isAddNew = false,
                                signerType = signer.type
                            )
                        ),
                        groupId = ""
                    )
                )
            } else {
                savedStateHandle[KEY_CURRENT_SIGNER] = signer
                _event.emit(AddKeyListEvent.SelectAirgapType)
            }
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    val result =
                        updateRemoteSignerUseCase.execute(singleSigner.copy(tags = listOf(tag)))
                    if (result is Result.Success) {
                        savedStateHandle[KEY_CURRENT_SIGNER] = null
                        loadSigners()
                        onSelectedExistingHardwareSigner(signer.copy(tags = listOf(tag)))
                    } else {
                        _event.emit(AddKeyListEvent.ShowError((result as Result.Error).exception.message.orUnknownError()))
                    }
                }
        }
    }

    fun getUpdateSigner(): SignerModel? = savedStateHandle.get<SignerModel>(KEY_CURRENT_SIGNER)

    fun onAddKeyClicked(data: AddKeyData) {
        viewModelScope.launch {
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            _event.emit(AddKeyListEvent.OnAddKey(data))
        }
    }

    private fun isSignerExist(masterSignerId: String) = membershipStepManager.isKeyExisted(masterSignerId)

    fun onVerifyClicked(data: AddKeyData) {
        data.signer?.let { signer ->
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            viewModelScope.launch {
                val stepInfo = getStepInfo(data.type)
                _event.emit(
                    AddKeyListEvent.OnVerifySigner(
                        signer = signer,
                        filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer)
                    )
                )
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(AddKeyListEvent.OnAddAllKey)
        }
    }

    private fun getStepInfo(step: MembershipStep) =
        membershipStepState.value.find { it.step == step } ?: run {
            MembershipStepInfo(step = step, plan = membershipStepManager.plan, groupId = "")
        }

    fun getTapSigners() =
        _state.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _state.value.signers.filter {
        it.type == SignerType.COLDCARD_NFC
                && it.derivationPath.contains(SIGNER_PATH_PREFIX)
                && isSignerExist(it.fingerPrint).not()
    }

    fun getAirgap() =
        _state.value.signers.filter { it.type == SignerType.AIRGAP && isSignerExist(it.fingerPrint).not() }

    fun reuseKeyFromWallet(id: String) {
        viewModelScope.launch {
            val result = reuseKeyWalletUseCase(ReuseKeyWalletUseCase.Param(id, membershipStepManager.plan))
            if (result.isFailure) {
                _event.emit(AddKeyListEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_CURRENT_SIGNER = "current_signer"
    }
}

sealed class AddKeyListEvent {
    data class OnAddKey(val data: AddKeyData) : AddKeyListEvent()
    data class OnVerifySigner(val signer: SignerModel, val filePath: String) : AddKeyListEvent()
    object OnAddAllKey : AddKeyListEvent()
    object SelectAirgapType : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
}

data class AddKeyListState(val signers: List<SignerModel> = emptyList())