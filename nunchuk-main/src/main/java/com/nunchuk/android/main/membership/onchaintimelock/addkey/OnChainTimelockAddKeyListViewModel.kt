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

package com.nunchuk.android.main.membership.onchaintimelock.addkey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.main.membership.model.AddKeyOnChainData
import com.nunchuk.android.main.membership.model.toAddKeyOnChainDataList
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.main.membership.model.toSteps
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.membership.CheckRequestAddDesktopKeyStatusUseCase
import com.nunchuk.android.usecase.membership.GetMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SaveMembershipStepUseCase
import com.nunchuk.android.usecase.membership.SyncDraftWalletUseCase
import com.nunchuk.android.usecase.membership.SyncKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetCurrentSignerIndexUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnChainTimelockAddKeyListViewModel @Inject constructor(
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
    private val syncKeyUseCase: SyncKeyUseCase,
    private val syncDraftWalletUseCase: SyncDraftWalletUseCase,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val getCurrentSignerIndexUseCase: GetCurrentSignerIndexUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val pushEventManager: com.nunchuk.android.core.push.PushEventManager,
) : ViewModel() {
    private val _state = MutableStateFlow(AddKeyListState())
    val state = _state.asStateFlow()
    private val _event = MutableSharedFlow<AddKeyListEvent>()
    val event = _event.asSharedFlow()
    private var loadJob: Job? = null

    private val currentStep =
        savedStateHandle.getStateFlow<MembershipStep?>(KEY_CURRENT_STEP, null)

    private val membershipStepState =
        getMembershipStepUseCase(
            GetMembershipStepUseCase.Param(
                membershipStepManager.localMembershipPlan,
                ""
            )
        ).map { it.getOrElse { emptyList() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _keys = MutableStateFlow(listOf<AddKeyOnChainData>())
    val key = _keys.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()

    // Context for TapSigner caching
    private var pendingTapSignerData: AddKeyOnChainData? = null
    private var pendingTapSignerWalletId: String? = null
    private var pendingTapSignerSignerModel: SignerModel? = null

    init {
        viewModelScope.launch {
            currentStep.filterNotNull().collect {
                membershipStepManager.setCurrentStep(it)
            }
        }
        viewModelScope.launch {
            loadSigners()
        }
        viewModelScope.launch {
            membershipStepState.combine(key) { _, keys -> keys }
                .collect { keys ->
                    val updatedKeys = keys.map { addKeyData ->
                        var updatedCard = addKeyData

                        // Process each step in the card
                        addKeyData.steps.forEach { step ->
                            val info = getStepInfo(step)
                            val extra = runCatching {
                                gson.fromJson(
                                    info.extraData,
                                    SignerExtra::class.java
                                )
                            }.getOrNull()

                            // If step has a master signer ID and extra data, try to find and add the signer
                            if (info.masterSignerId.isNotEmpty() && extra != null) {
                                loadSigners()
                                var signer =
                                    _state.value.signers.find { it.fingerPrint == info.masterSignerId }
                                signer = signer?.copy(
                                    index = getIndexFromPathUseCase(extra.derivationPath)
                                        .getOrDefault(0),
                                    derivationPath = extra.derivationPath.ifEmpty { signer.derivationPath }
                                )

                                if (signer != null) {
                                    updatedCard =
                                        updatedCard.updateStep(step, signer, info.verifyType)
                                }
                            } else if (step == MembershipStep.ADD_SEVER_KEY || step == MembershipStep.TIMELOCK) {
                                updatedCard = updatedCard.updateStep(
                                    step,
                                    null,
                                    info.verifyType,
                                    timelock = if (step == MembershipStep.TIMELOCK) info.parseTimelockExtra() else null
                                )
                            }
                        }

                        updatedCard
                    }
                    _keys.value = updatedKeys
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
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.DraftWalletTimelockSet -> {
                        refresh()
                    }

                    else -> {}
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isRefresh = true) }
            syncDraftWalletUseCase("").onSuccess { draft ->
                loadSigners()
                _state.update { it.copy(walletType = WalletType.MINISCRIPT) }
                draft.config.toGroupWalletType()?.let { type ->
                    if (_keys.value.isEmpty()) {
                        val steps = type.toSteps(
                            isPersonalWallet = true,
                            walletType = WalletType.MINISCRIPT
                        )
                        _keys.value = steps.toAddKeyOnChainDataList()
                    }
                }
            }
            _state.update { it.copy(isRefresh = false) }
        }
    }

    private suspend fun loadSigners() {
        getAllSignersUseCase(false).onSuccess { pair ->
            _state.update {
                singleSigners.apply {
                    clear()
                    addAll(pair.second)
                }
                masterSigners.apply {
                    clear()
                    addAll(pair.first)
                }
                it.copy(
                    signers = pair.first.map { signer ->
                        masterSignerMapper(signer)
                    } + pair.second.map { signer -> signer.toModel() }
                )
            }
        }
    }

    fun onSelectedExistingHardwareSigner(signer: SingleSigner) {
        viewModelScope.launch {
            val actualSigner = if (signer.xpub.isNotEmpty()) signer else singleSigners.find {
                it.masterFingerprint == signer.masterFingerprint
                        && it.derivationPath == signer.derivationPath
            } ?: return@launch

            val currentStep = membershipStepManager.currentStep
                ?: throw IllegalArgumentException("Current step empty")

            syncKeyUseCase(
                SyncKeyUseCase.Param(
                    step = currentStep,
                    signer = actualSigner,
                    walletType = WalletType.MINISCRIPT
                )
            ).onFailure {
                _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
                return@launch
            }
            saveMembershipStepUseCase(
                MembershipStepInfo(
                    step = currentStep,
                    masterSignerId = signer.masterFingerprint,
                    plan = membershipStepManager.localMembershipPlan,
                    verifyType = VerifyType.APP_VERIFIED,
                    extraData = gson.toJson(
                        SignerExtra(
                            derivationPath = signer.derivationPath,
                            isAddNew = false,
                            signerType = signer.type,
                            userKeyFileName = ""
                        )
                    ),
                    groupId = ""
                )
            )

            // Update the card with the new signer for the current step
            updateCardForStep(currentStep, signer.toModel(), VerifyType.APP_VERIFIED)
        }
    }

    private fun updateCardForStep(
        step: MembershipStep,
        signer: SignerModel,
        verifyType: VerifyType
    ) {
        val currentKeys = _keys.value
        val cardIndex = currentKeys.indexOfFirst { it.steps.contains(step) }

        if (cardIndex != -1) {
            val card = currentKeys[cardIndex]
            val updatedCard = card.updateStep(step, signer, verifyType)
            _keys.value = currentKeys.toMutableList().apply {
                set(cardIndex, updatedCard)
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
                        loadSigners()
                        onSelectedExistingHardwareSigner(singleSigner.copy(tags = listOf(tag)))
                    } else {
                        _event.emit(AddKeyListEvent.ShowError((result as Result.Error).exception.message.orUnknownError()))
                    }
                }
        }
    }

    fun onAddKeyClicked(data: AddKeyOnChainData) {
        viewModelScope.launch {
            // Get the next step that needs to be added
            val nextStep = data.getNextStepToAdd()
            if (nextStep != null) {
                savedStateHandle[KEY_CURRENT_STEP] = nextStep
                _event.emit(AddKeyListEvent.OnAddKey(data))
            }
        }
    }

    private fun isSignerExist(masterSignerId: String) =
        membershipStepManager.isKeyExisted(masterSignerId)

    fun onVerifyClicked(data: AddKeyOnChainData) {
        viewModelScope.launch {
            // Find the first step that needs verification (has signer but needs verification)
            val stepToVerify = data.steps.firstOrNull { step ->
                val stepData = data.stepDataMap[step]
                stepData?.signer != null && stepData.verifyType == VerifyType.NONE
            } ?: data.steps.firstOrNull { step ->
                data.stepDataMap[step]?.signer != null
            } ?: return@launch

            val signer = data.getSignerForStep(stepToVerify) ?: return@launch

            savedStateHandle[KEY_CURRENT_STEP] = stepToVerify
            val stepInfo = getStepInfo(stepToVerify)
            _event.emit(
                AddKeyListEvent.OnVerifySigner(
                    signer = signer,
                    filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer),
                    backUpFileName = getBackUpFileName(stepInfo.extraData)
                )
            )
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            refresh()
            _event.emit(AddKeyListEvent.OnAddAllKey)
        }
    }

    fun requestCacheTapSignerXpub() {
        _state.update { it.copy(requestCacheTapSignerXpubEvent = true) }
    }

    fun resetRequestCacheTapSignerXpub() {
        _state.update { it.copy(requestCacheTapSignerXpubEvent = false) }
    }

    fun cacheTapSignerXpub(isoDep: android.nfc.tech.IsoDep?, cvc: String) {
        val masterSignerId = savedStateHandle.get<String>(KEY_TAPSIGNER_MASTER_ID) ?: return
        val path = savedStateHandle.get<String>(KEY_TAPSIGNER_PATH) ?: return
        val contextName = savedStateHandle.get<String>(KEY_TAPSIGNER_CONTEXT) ?: return

        isoDep ?: return

        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("cacheTapSignerXpub: currentStep=${membershipStepManager.currentStep}, pendingTapSignerData=$pendingTapSignerData, masterSignerId=$masterSignerId, path=$path, context=$contextName")
            getSignerFromTapsignerMasterSignerByPathUseCase(
                GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                    isoDep = isoDep,
                    masterSignerId = masterSignerId,
                    path = path,
                    cvc = cvc
                )
            ).onSuccess { newSigner ->
                val newSignerModel = singleSignerMapper(newSigner)

                // Resume the appropriate flow based on context
                val context = TapSignerCachingContext.valueOf(contextName)
                when (context) {
                    TapSignerCachingContext.ADD_TAPSIGNER_KEY -> {
                        // Resume addTapSignerKey flow - note: this is for Acct 0, don't call handleTapSignerAcct1Addition
                        processTapSignerWithCompleteData(
                            newSigner,
                            pendingTapSignerSignerModel ?: newSignerModel,
                            pendingTapSignerData,
                            pendingTapSignerWalletId
                        )
                    }

                    TapSignerCachingContext.HANDLE_ACCT1_ADDITION -> {
                        // Resume handleTapSignerAcct1Addition flow - this is for Acct 1, don't call handleTapSignerAcct1Addition again
                        processTapSignerWithCompleteData(newSigner, newSignerModel)
                    }

                    TapSignerCachingContext.HANDLE_CUSTOM_KEY_ACCOUNT_RESULT -> {
                        // Resume handleCustomKeyAccountResult flow
                        processTapSignerWithCompleteData(newSigner, newSignerModel)
                    }
                }

                // Clear context
                clearTapSignerCachingContext()
            }.onFailure { error ->
                _event.emit(AddKeyListEvent.ShowError(error.message.orUnknownError()))
                clearTapSignerCachingContext()
            }

            resetRequestCacheTapSignerXpub()
        }
    }

    private fun clearTapSignerCachingContext() {
        savedStateHandle.remove<String>(KEY_TAPSIGNER_MASTER_ID)
        savedStateHandle.remove<String>(KEY_TAPSIGNER_PATH)
        savedStateHandle.remove<String>(KEY_TAPSIGNER_CONTEXT)
        pendingTapSignerData = null
        pendingTapSignerWalletId = null
        pendingTapSignerSignerModel = null
    }

    fun addExistingTapSignerKey(
        signerModel: SignerModel,
        data: AddKeyOnChainData? = null,
        walletId: String? = null
    ) {
        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("addExistingTapSignerKey: currentStep=${membershipStepManager.currentStep}, data=$data, signerModel=$signerModel, walletId=$walletId")
            if (signerModel.isMasterSigner && signerModel.type == SignerType.NFC) {
                var masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                if (masterSigner == null) {
                    loadSigners()
                    masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                }
                if (masterSigner == null) {
                    _event.emit(AddKeyListEvent.ShowError("Master signer not found with id=${signerModel.fingerPrint}"))
                    return@launch
                }

                // Use GetUnusedSignerFromMasterSignerV2UseCase to get signer with complete data
                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        AddressType.NATIVE_SEGWIT
                    )
                ).onSuccess { singleSigner ->
                    // Process the signer with complete data
                    processTapSignerWithCompleteData(singleSigner, signerModel, data, walletId)
                }.onFailure { error ->
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        // Store context for TapSigner caching - using index 0 for first account
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = signerModel.fingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(0)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.ADD_TAPSIGNER_KEY.name
                        pendingTapSignerData = data
                        pendingTapSignerWalletId = walletId
                        pendingTapSignerSignerModel = signerModel
                        requestCacheTapSignerXpub()
                    } else {
                        _event.emit(AddKeyListEvent.ShowError(error.message.orUnknownError()))
                    }
                }
            } else {
                _event.emit(AddKeyListEvent.ShowError("Invalid signer type for TapSigner addition"))
            }
        }
    }

    private suspend fun processTapSignerWithCompleteData(
        signer: SingleSigner,
        signerModel: SignerModel,
        data: AddKeyOnChainData? = null,
        walletId: String? = null
    ) {
        Timber.tag("tapsigner-onchain")
            .d("processTapSignerWithCompleteData: currentStep=${membershipStepManager.currentStep}, data=$data, signer=$signer, signerModel=$signerModel, walletId=$walletId")
        // Sync the signer to the membership system
        syncKeyUseCase(
            SyncKeyUseCase.Param(
                step = membershipStepManager.currentStep
                    ?: throw IllegalArgumentException("Current step empty"),
                signer = signer,
                walletType = WalletType.MINISCRIPT
            )
        ).onFailure {
            _event.emit(AddKeyListEvent.ShowError(it.message.orUnknownError()))
            return
        }

        val currentStep = membershipStepManager.currentStep
            ?: throw IllegalArgumentException("Current step empty")
        val verifyType = if (signer.type == SignerType.NFC) VerifyType.NONE else VerifyType.APP_VERIFIED
        // Save membership step
        saveMembershipStepUseCase(
            MembershipStepInfo(
                step = currentStep,
                masterSignerId = signer.masterFingerprint,
                plan = membershipStepManager.localMembershipPlan,
                verifyType = verifyType,
                extraData = gson.toJson(
                    SignerExtra(
                        derivationPath = signer.derivationPath,
                        isAddNew = false,
                        signerType = signer.type,
                        userKeyFileName = ""
                    )
                ),
                groupId = ""
            )
        )

        // Update the card with the new signer for the current step
        updateCardForStep(currentStep, signerModel, verifyType)

        // After successfully adding signer, handle TapSigner Acct 1 addition if we have the required data
        val nextStep = data?.getNextStepToAdd(currentStep)
        if (nextStep != null && walletId != null) {
            savedStateHandle[KEY_CURRENT_STEP] = nextStep
            membershipStepManager.setCurrentStep(nextStep)
            handleTapSignerAcct1Addition(data, signerModel, walletId)
        }
    }

    fun handleTapSignerAcct1Addition(
        data: AddKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            Timber.tag("tapsigner-onchain")
                .d("handleTapSignerAcct1Addition: currentStep=${membershipStepManager.currentStep}, data=$data, firstSigner=$firstSigner, walletId=$walletId")
            // Get current index from master signer
            val currentIndexResult = getCurrentIndexFromMasterSigner(firstSigner.fingerPrint)

            when (currentIndexResult) {
                is Result.Success -> {
                    if (currentIndexResult.data >= 1) {
                        // Navigate to CustomKeyAccountFragment
                        val onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_SIGNER,
                            keyIndex = data.signers?.size ?: 0,
                            currentSigner = firstSigner
                        )
                        _event.emit(
                            AddKeyListEvent.NavigateToCustomKeyAccount(
                                firstSigner,
                                walletId,
                                onChainAddSignerParam
                            )
                        )
                        return@launch
                    } else {
                        // Current index < 1, try to get signer at index 1
                        val signerByIndexResult =
                            getSignerFromMasterSignerByIndex(firstSigner.fingerPrint, 1)

                        when (signerByIndexResult) {
                            is Result.Success -> {
                                // Successfully got signer at index 1, process it with complete data
                                signerByIndexResult.data?.let { singleSigner ->
                                    processTapSignerWithCompleteData(
                                        singleSigner,
                                        singleSigner.toModel()
                                    )
                                }
                            }

                            is Result.Error -> {
                                val error = signerByIndexResult.exception
                                if (error is NCNativeException && error.message.contains("-1009") == true) {
                                    // Store context for TapSigner caching - using index 1 for second account
                                    savedStateHandle[KEY_TAPSIGNER_MASTER_ID] =
                                        firstSigner.fingerPrint
                                    savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(1)
                                    savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                                        TapSignerCachingContext.HANDLE_ACCT1_ADDITION.name
                                    pendingTapSignerData = data
                                    pendingTapSignerWalletId = walletId
                                    pendingTapSignerSignerModel = firstSigner
                                    requestCacheTapSignerXpub()
                                } else {
                                    // Other error, show error message
                                    _event.emit(
                                        AddKeyListEvent.ShowError(
                                            error.message ?: "Failed to get signer at index 1"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                is Result.Error -> {
                    // Error getting current index
                    _event.emit(
                        AddKeyListEvent.ShowError(
                            currentIndexResult.exception.message ?: "Failed to get current index"
                        )
                    )
                }
            }
        }
    }

    fun handleCustomKeyAccountResult(signerFingerPrint: String, newIndex: Int) {
        viewModelScope.launch {
            val signerByIndexResult = getSignerFromMasterSignerByIndex(signerFingerPrint, newIndex)

            when (signerByIndexResult) {
                is Result.Success -> {
                    signerByIndexResult.data?.let { singleSigner ->
                        processTapSignerWithCompleteData(singleSigner, singleSigner.toModel())
                    }
                }

                is Result.Error -> {
                    val error = signerByIndexResult.exception
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        // Store context for TapSigner caching - using the custom newIndex
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = signerFingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(newIndex)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.HANDLE_CUSTOM_KEY_ACCOUNT_RESULT.name
                        // For custom key account result, we don't have data/walletId context to store
                        pendingTapSignerData = null
                        pendingTapSignerWalletId = null
                        pendingTapSignerSignerModel = null
                        requestCacheTapSignerXpub()
                    } else {
                        // Other error, show error message
                        _event.emit(
                            AddKeyListEvent.ShowError(
                                error.message ?: "Failed to get signer at index $newIndex"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getStepInfo(step: MembershipStep) =
        membershipStepState.value.filter {
            it.plan == membershipStepManager.localMembershipPlan
        }.find { it.step == step } ?: run {
            MembershipStepInfo(
                step = step,
                plan = membershipStepManager.localMembershipPlan,
                groupId = ""
            )
        }

    fun getHardwareSigners(tag: SignerTag) =
        _state.value.signers.filter {
            isSignerExist(it.fingerPrint).not() && it.type == SignerType.HARDWARE && it.tags.contains(
                tag
            )
        }

    private fun getBackUpFileName(extra: String): String {
        return runCatching {
            gson.fromJson(extra, SignerExtra::class.java).userKeyFileName
        }.getOrDefault("")
    }

    /**
     * Gets the current index from a master signer
     */
    suspend fun getCurrentIndexFromMasterSigner(fingerPrint: String): Result<Int> {
        return runCatching {
            getCurrentSignerIndexUseCase(
                GetCurrentSignerIndexUseCase.Param(
                    masterSignerId = fingerPrint,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it as Exception) }
        )
    }

    /**
     * Gets a signer from master signer by specific index (for TapSigner flows)
     */
    suspend fun getSignerFromMasterSignerByIndex(
        fingerPrint: String,
        index: Int
    ): Result<SingleSigner?> {
        return runCatching {
            getSignerFromMasterSignerByIndexUseCase(
                GetSignerFromMasterSignerByIndexUseCase.Param(
                    masterSignerId = fingerPrint,
                    index = index,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it as Exception) }
        )
    }

    /**
     * Gets a remote signer by specific index using derivation path
     */
    suspend fun getRemoteSignerByIndex(
        fingerPrint: String,
        index: Int
    ): Result<SingleSigner?> {
        return runCatching {
            val derivationPath = getPath(index)
            getRemoteSignerUseCase(
                GetRemoteSignerUseCase.Data(
                    id = fingerPrint,
                    derivationPath = derivationPath
                )
            ).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it as Exception) }
        )
    }

    /**
     * Handles the logic for checking signer index and navigating accordingly
     * @param firstSigner The first signer to check
     * @param walletId The wallet ID for navigation
     */
    fun handleSignerIndexCheck(
        data: AddKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            // Call getCurrentIndexFromMasterSigner to check resultIndex
            val resultIndexResult = getCurrentIndexFromMasterSigner(firstSigner.fingerPrint)

            when (resultIndexResult) {
                is Result.Success -> {
                    val resultIndex = resultIndexResult.data

                    if (resultIndex >= 1) {
                        // If resultIndex >= 1, navigate to CustomKeyAccountFragment
                        _event.emit(
                            AddKeyListEvent.NavigateToCustomKeyAccount(
                                signer = firstSigner,
                                walletId = walletId,
                                onChainAddSignerParam = OnChainAddSignerParam(
                                    flags = if (data.type.isAddInheritanceKey) OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER else OnChainAddSignerParam.FLAG_ADD_SIGNER,
                                    keyIndex = 1,
                                    currentSigner = firstSigner
                                )
                            )
                        )
                    } else {
                        // If resultIndex < 1, call GetRemoteSignerUseCase
                        val signerResult =
                            getRemoteSignerByIndex(firstSigner.fingerPrint, 1)

                        when (signerResult) {
                            is Result.Success -> {
                                val signer = signerResult.data

                                if (signer != null) {
                                    // If signer != null, add signer to corresponding AddKeyOnChainData.signers
                                    onSelectedExistingHardwareSigner(signer)
                                } else {
                                    // If signer == null, run handleSignerTypeLogic flow
                                    _event.emit(
                                        AddKeyListEvent.HandleSignerTypeLogic(
                                            firstSigner.type,
                                            firstSigner.tags.first { it != SignerTag.INHERITANCE }
                                        )
                                    )
                                }
                            }

                            is Result.Error -> {
                                // If error getting signer, run handleSignerTypeLogic flow
                                _event.emit(
                                    AddKeyListEvent.HandleSignerTypeLogic(
                                        firstSigner.type,
                                        firstSigner.tags.first { it != SignerTag.INHERITANCE }
                                    )
                                )
                            }
                        }
                    }
                }

                is Result.Error -> {
                    // If error getting current index, show error
                    _event.emit(
                        AddKeyListEvent.ShowError(
                            resultIndexResult.exception.message ?: "Failed to get current index"
                        )
                    )
                }
            }
        }
    }

    private fun getPath(
        index: Int,
        isTestNet: Boolean = false,
        isMultisig: Boolean = true
    ): String {
        if (isMultisig) {
            return if (isTestNet) "m/48h/1h/${index}h/2h" else "m/48h/0h/${index}h/2h"
        }
        return if (isTestNet) "m/84h/1h/${index}h" else "m/84h/0h/${index}h"
    }

    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_TAPSIGNER_MASTER_ID = "tapsigner_master_id"
        private const val KEY_TAPSIGNER_PATH = "tapsigner_path"
        private const val KEY_TAPSIGNER_CONTEXT = "tapsigner_context"
    }

    private enum class TapSignerCachingContext {
        ADD_TAPSIGNER_KEY,
        HANDLE_ACCT1_ADDITION,
        HANDLE_CUSTOM_KEY_ACCOUNT_RESULT
    }
}

sealed class AddKeyListEvent {
    data class OnAddKey(val data: AddKeyOnChainData) : AddKeyListEvent()
    data class OnVerifySigner(
        val signer: SignerModel,
        val filePath: String,
        val backUpFileName: String
    ) : AddKeyListEvent()

    data object OnAddAllKey : AddKeyListEvent()
    data object SelectAirgapType : AddKeyListEvent()
    data class NavigateToCustomKeyAccount(
        val signer: SignerModel,
        val walletId: String,
        val onChainAddSignerParam: OnChainAddSignerParam? = null
    ) : AddKeyListEvent()

    data class HandleSignerTypeLogic(val type: SignerType, val tag: SignerTag?) : AddKeyListEvent()
    data class ShowError(val message: String) : AddKeyListEvent()
}

data class AddKeyListState(
    val isLoading: Boolean = false,
    val isRefresh: Boolean = false,
    val signers: List<SignerModel> = emptyList(),
    val walletType: WalletType? = null,
    val requestCacheTapSignerXpubEvent: Boolean = false
)
