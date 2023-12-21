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

package com.nunchuk.android.main.membership.byzantine.addKey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.main.membership.model.toSteps
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.byzantine.FindSimilarGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.ReuseGroupWalletUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncGroupDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyToGroupUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddByzantineKeyListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getMembershipStepUseCase: GetMembershipStepUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val nfcFileManager: NfcFileManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val saveMembershipStepUseCase: SaveMembershipStepUseCase,
    private val gson: Gson,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val syncKeyToGroupUseCase: SyncKeyToGroupUseCase,
    private val syncGroupDraftWalletUseCase: SyncGroupDraftWalletUseCase,
    private val findSimilarGroupWalletUseCase: FindSimilarGroupWalletUseCase,
    private val reuseGroupWalletUseCase: ReuseGroupWalletUseCase,
    private val pushEventManager: PushEventManager,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()
    private val args: AddByzantineKeyListFragmentArgs =
        AddByzantineKeyListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState = getMembershipStepUseCase(
        GetMembershipStepUseCase.Param(
            membershipStepManager.plan,
            args.groupId
        )
    ).map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()

    init {
        if (args.isAddOnly.not() && membershipStepManager.isNotConfig()) {
            viewModelScope.launch {
                findSimilarGroupWalletUseCase(args.groupId).onSuccess { similar ->
                    _state.update { it.copy(similarGroups = similar.associate { similar -> similar.walletLocalId to similar.groupId }) }
                    if (similar.isNotEmpty()) {
                        _event.emit(AddKeyListEvent.LoadSimilarGroup(similar.map { it.walletLocalId }))
                    }
                }
            }
        }
        if (args.isAddOnly) {
            viewModelScope.launch {
                pushEventManager.event.filterIsInstance<PushEvent.KeyAddedToGroup>().collect {
                    _state.update { it.copy(shouldShowKeyAdded = true) }
                }
            }
        }
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        refresh()
        viewModelScope.launch {
            membershipStepState.combine(key) { _, key -> key }.collect {
                loadSigners()
                updateKeyData()
            }
        }
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(Unit).onSuccess { pair ->
            singleSigners.apply {
                clear()
                addAll(pair.second)
            }
            val signers = pair.first.map { signer ->
                masterSignerMapper(signer)
            } + pair.second.map { signer -> signer.toModel() }
            _state.update { it.copy(signers = signers) }
            updateKeyData()
        }
    }

    private suspend fun updateKeyData() {
        if (key.value.isEmpty()) return
        val signers = _state.value.`signers`
        val news = key.value.map { addKeyData ->
            val info = getStepInfo(addKeyData.type)
            var signer = if (info.masterSignerId.isNotEmpty()) signers.find { it.fingerPrint == info.masterSignerId } else null
            if (signer != null) {
                runCatching {
                    val extra = gson.fromJson(info.extraData, SignerExtra::class.java)
                    signer = signer?.copy(index = getIndexFromPathUseCase(extra.derivationPath).getOrDefault(0))
                }
            }
            addKeyData.copy(
                signer = signer,
                verifyType = info.verifyType
            )
        }
        _keys.value = news
    }

    fun requestAddAirgapTag(signer: SignerModel) {
        viewModelScope.launch {
            savedStateHandle[KEY_CURRENT_SIGNER] = signer
            _event.emit(AddKeyListEvent.SelectAirgapType)
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
                        _event.emit(AddKeyListEvent.UpdateSignerTag(signer.copy(tags = listOf(tag))))
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
            MembershipStepInfo(step = step, plan = membershipStepManager.plan, groupId = "")
        }

    fun getTapSigners() =
        _state.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _state.value.signers.filter {
        it.type == SignerType.COLDCARD_NFC
                && it.derivationPath.isRecommendedPath
                && isSignerExist(it.fingerPrint).not()
    }

    fun getAirgap() =
        _state.value.signers.filter { it.type == SignerType.AIRGAP && isSignerExist(it.fingerPrint).not() }

    fun getHardwareSigners(tag: SignerTag) =
        _state.value.signers.filter { it.type == SignerType.HARDWARE && it.tags.contains(tag) }

    fun reuseKeyFromWallet(id: String) {
        viewModelScope.launch {
            reuseGroupWalletUseCase(
                ReuseGroupWalletUseCase.Params(
                    args.groupId,
                    state.value.similarGroups[id].orEmpty()
                )
            ).onFailure {
                _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            syncGroupDraftWalletUseCase(args.groupId).onSuccess { draft ->
                loadSigners()
                _state.update { it.copy(groupWalletType = draft.config.toGroupWalletType()) }
                draft.config.toGroupWalletType()?.let { type ->
                    if (_keys.value.isEmpty()) {
                        _keys.value = type.toSteps().map { step -> AddKeyData(type = step) }
                    }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun markHandledShowKeyAdded() {
        _state.update { it.copy(shouldShowKeyAdded = false) }
    }

    fun handleSignerNewIndex(signer: SingleSigner) {
        viewModelScope.launch {
            syncKeyToGroupUseCase(
                SyncKeyToGroupUseCase.Param(
                    groupId = args.groupId,
                    step = membershipStepManager.currentStep
                        ?: throw IllegalArgumentException("Current step empty"),
                    signer = signer
                )
            ).onFailure {
                _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
                return@launch
            }
            saveMembershipStepUseCase(
                MembershipStepInfo(
                    step = membershipStepManager.currentStep
                        ?: throw IllegalArgumentException("Current step empty"),
                    masterSignerId = signer.masterFingerprint,
                    plan = membershipStepManager.plan,
                    verifyType = VerifyType.APP_VERIFIED,
                    extraData = gson.toJson(
                        SignerExtra(
                            derivationPath = signer.derivationPath,
                            isAddNew = false,
                            signerType = signer.type
                        )
                    ),
                    groupId = args.groupId
                )
            )
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
    data object OnAddAllKey : AddKeyListEvent()
    data object SelectAirgapType : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
    data class LoadSimilarGroup(val similarWalletIds: List<String>) : AddKeyListEvent()
    data class UpdateSignerTag(val signer: SignerModel) : AddKeyListEvent()
}

data class AddKeyListState(
    val isRefreshing: Boolean = false,
    val signers: List<SignerModel> = emptyList(),
    val similarGroups: Map<String, String> = emptyMap(),
    val shouldShowKeyAdded: Boolean = false,
    val groupWalletType: GroupWalletType? = null,
)