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
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddKeyListViewModel @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val checkRequestAddDesktopKeyStatusUseCase: CheckRequestAddDesktopKeyStatusUseCase,
    private val ncDataStore: NcDataStore
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState =
        getMembershipStepUseCase(
            GetMembershipStepUseCase.Param(
                membershipStepManager.localMembershipPlan,
                ""
            )
        )
            .map { it.getOrElse { emptyList() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    var shouldShowNewPortal: Boolean = false

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        if (membershipStepManager.localMembershipPlan == MembershipPlan.IRON_HAND) {
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
        viewModelScope.launch {
            loadSigners()
        }
        viewModelScope.launch {
            membershipStepState.collect {
                val news = _keys.value.map { addKeyData ->
                    val info = getStepInfo(addKeyData.type)
                    val extra = runCatching { gson.fromJson(info.extraData, SignerExtra::class.java) }.getOrNull()
                    if (addKeyData.signer == null && info.masterSignerId.isNotEmpty()) {
                        loadSigners()
                        val path = extra?.derivationPath.orEmpty()
                        val signer = _state.value.signers.find { it.fingerPrint == info.masterSignerId && it.derivationPath == path }
                        return@map addKeyData.copy(
                            signer = signer,
                            verifyType = info.verifyType
                        )
                    }
                    addKeyData.copy(verifyType = info.verifyType)
                }
                _keys.value = news
            }
        }
        viewModelScope.launch {
            checkRequestAddDesktopKeyStatusUseCase(
                CheckRequestAddDesktopKeyStatusUseCase.Param(
                    membershipStepManager.localMembershipPlan
                )
            )
        }
        viewModelScope.launch {
            shouldShowNewPortal = ncDataStore.shouldShowNewPortal()
        }
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(false).onSuccess { pair ->
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

    // COLDCARD or Airgap
    fun onSelectedExistingHardwareSigner(signer: SignerModel) {
        viewModelScope.launch {
            saveMembershipStepUseCase(
                MembershipStepInfo(
                    step = membershipStepManager.currentStep
                        ?: throw IllegalArgumentException("Current step empty"),
                    masterSignerId = signer.fingerPrint,
                    plan = membershipStepManager.localMembershipPlan,
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
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    val result =
                        updateRemoteSignerUseCase.execute(singleSigner.copy(tags = listOf(tag)))
                    if (result is Result.Success) {
                        loadSigners()
                        onSelectedExistingHardwareSigner(signer.copy(tags = listOf(tag)))
                    } else {
                        _event.emit(AddKeyListEvent.ShowError((result as Result.Error).exception.message.orUnknownError()))
                    }
                }
        }
    }

    fun onAddKeyClicked(data: AddKeyData) {
        viewModelScope.launch {
            savedStateHandle[KEY_CURRENT_STEP] = data.type
            _event.emit(AddKeyListEvent.OnAddKey(data))
        }
    }

    private fun isSignerExist(masterSignerId: String) =
        membershipStepManager.isKeyExisted(masterSignerId)

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
            MembershipStepInfo(
                step = step,
                plan = membershipStepManager.localMembershipPlan,
                groupId = ""
            )
        }

    fun getTapSigners() =
        _state.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _state.value.signers.filter {
        isSignerExist(it.fingerPrint).not()
                && ((it.type == SignerType.COLDCARD_NFC && it.derivationPath.isRecommendedMultiSigPath)
                || (it.type == SignerType.AIRGAP && (it.tags.isEmpty() || it.tags.contains(SignerTag.COLDCARD))))
    }

    fun getHardwareSigners(tag: SignerTag) =
        _state.value.signers.filter {
            isSignerExist(it.fingerPrint).not() && it.type == SignerType.HARDWARE && it.tags.contains(
                tag
            )
        }

    fun getAirgap(tag: SignerTag?): List<SignerModel> {
        return if (tag == null) {
            _state.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
            }
        } else {
            _state.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
                        && (it.tags.contains(tag) || it.tags.isEmpty())
            }
        }
    }

    fun getPortal(): List<SignerModel> =
        _state.value.signers.filter { it.type == SignerType.PORTAL_NFC && isSignerExist(it.fingerPrint).not() }

    fun markShowPortal() {
        viewModelScope.launch {
            ncDataStore.setShowPortal(false)
        }
    }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
    }
}

sealed class AddKeyListEvent {
    data class OnAddKey(val data: AddKeyData) : AddKeyListEvent()
    data class OnVerifySigner(val signer: SignerModel, val filePath: String) : AddKeyListEvent()
    data object OnAddAllKey : AddKeyListEvent()
    data object SelectAirgapType : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
}

data class AddKeyListState(val signers: List<SignerModel> = emptyList())