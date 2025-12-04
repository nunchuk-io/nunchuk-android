package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CreateWallet2UseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletsUseCase
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.main.membership.model.AddReplaceKeyOnChainData
import com.nunchuk.android.main.membership.model.ReplaceStepData
import com.nunchuk.android.main.membership.model.toAddReplaceKeyOnChainDataList
import com.nunchuk.android.main.membership.replacekey.ReplaceKeysFragmentArgs
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.OnChainReplaceKeyStep
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.getReplaceKeySteps
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.model.toOnChainReplaceKeyStep
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetIndexFromPathUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.replace.FinalizeReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import com.nunchuk.android.usecase.replace.InitReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ResetReplaceKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import com.nunchuk.android.usecase.signer.RemoveKeyReplacementUseCase
import com.nunchuk.android.usecase.wallet.GetServerWalletUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class OnChainReplaceKeysViewModel @Inject constructor(
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val createWallet2UseCase: CreateWallet2UseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val accountManager: AccountManager,
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
    private val finalizeReplaceKeyUseCase: FinalizeReplaceKeyUseCase,
    private val initReplaceKeyUseCase: InitReplaceKeyUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val resetReplaceKeyUseCase: ResetReplaceKeyUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val nfcFileManager: NfcFileManager,
    private val singleSignerMapper: SingleSignerMapper,
    private val getServerWalletsUseCase: GetServerWalletsUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val pushEventManager: PushEventManager,
    private val getIndexFromPathUseCase: GetIndexFromPathUseCase,
    private val removeKeyReplacementUseCase: RemoveKeyReplacementUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
) : ViewModel() {
    private val args = ReplaceKeysFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _uiState =
        MutableStateFlow(OnChainReplaceKeysUiState())
    val uiState = _uiState.asStateFlow()
    private val _event = MutableSharedFlow<OnChainReplaceKeyEvent>()
    val event = _event.asSharedFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()

    private var isTestNet: Boolean = false

    // Context for TapSigner caching
    private var pendingTapSignerData: AddReplaceKeyOnChainData? = null
    private var pendingTapSignerWalletId: String? = null
    private var pendingTapSignerSignerModel: SignerModel? = null

    private val _keys = MutableStateFlow(listOf<AddReplaceKeyOnChainData>())
    val key = _keys.asStateFlow()

    private var loadWalletStatusJob: Job? = null

    init {
        viewModelScope.launch {
            pushEventManager.event.collect {
                if (it is PushEvent.ReplaceKeyChange) {
                    getReplaceWalletStatus()
                }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                val walletSigners =
                    wallet.signers.filter { signer -> signer.type != SignerType.SERVER }
                        .map { signer ->
                            singleSignerMapper(signer)
                        }
                _uiState.update {
                    it.copy(
                        walletSigners = walletSigners,
                        isMultiSig = wallet.signers.size > 1,
                        addressType = wallet.addressType,
                    )
                }
            }
        }
        if (args.groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupUseCase(
                    GetGroupUseCase.Params(args.groupId)
                ).map { it.getOrThrow() }
                    .distinctUntilChanged()
                    .collect { group ->
                        _uiState.update {
                            it.copy(group = group, myRole = currentUserRole(group.members))
                        }
                    }
            }
            viewModelScope.launch {
                syncGroupWalletUseCase(args.groupId).onSuccess { wallet ->
                    buildAddReplaceKeySteps(wallet.signers, wallet.timelock)
                }
            }
        } else {
            viewModelScope.launch {
                getServerWalletUseCase(args.walletId).onSuccess { wallet ->
                    buildAddReplaceKeySteps(wallet.signers, wallet.timelock)
                }
            }
        }
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    isTestNet = it == Chain.TESTNET
                }
        }
        getReplaceWalletStatus()
        loadSigners()
    }

    private suspend fun buildAddReplaceKeySteps(
        serverSigners: List<SignerServer>,
        timelock: WalletTimelock? = null
    ) {
        loadSigners()
        val steps = getReplaceKeySteps(isGroupWallet = args.groupId.isNotEmpty())
        val baseCards = steps.toAddReplaceKeyOnChainDataList()
        val cardsWithOriginals =
            baseCards.filter { it.type != OnChainReplaceKeyStep.SERVER_KEY && it.type != OnChainReplaceKeyStep.TIMELOCK }
                .map { card ->
                    val cardSigners = serverSigners.filter { signer ->
                        val stepForIndex = signer.index.toOnChainReplaceKeyStep(isGroupWallet = args.groupId.isNotEmpty())
                        card.steps.contains(stepForIndex)
                    }.map { signer -> signer.toModel(
                        getIndexFromPathUseCase(signer.derivationPath.orEmpty()).getOrDefault(0
                    )) }
                    card.copy(originalSigners = cardSigners, fingerPrint = cardSigners.firstOrNull()?.fingerPrint ?: "" )
                }
        val serverCard =
            baseCards.find { it.type == OnChainReplaceKeyStep.SERVER_KEY }!!.updateStep(
                step = OnChainReplaceKeyStep.SERVER_KEY,
                signer = null,
                verifyType = VerifyType.APP_VERIFIED,
            )

        val timelockCard =
            baseCards.find { it.type == OnChainReplaceKeyStep.TIMELOCK }!!.setOriginalTimelock(
                timelock = timelock!!
            )
        _keys.update { cardsWithOriginals + serverCard + timelockCard }
        getReplaceWalletStatus()
    }

    private fun loadSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(false).onSuccess { pair ->
                val singleSigner = pair.second.distinctBy { it.masterFingerprint }
                singleSigners.apply {
                    clear()
                    addAll(singleSigner)
                }
                masterSigners.apply {
                    clear()
                    addAll(pair.first)
                }
                val signers = pair.first.map { signer ->
                    masterSignerMapper(signer)
                } + singleSigner.map { signer -> signer.toModel() }
                _uiState.update { it.copy(signers = signers) }
            }
        }
    }

    fun getReplaceWalletStatus() {
        if (loadWalletStatusJob?.isActive == true) return
        loadWalletStatusJob = viewModelScope.launch {
            getReplaceWalletStatusUseCase(
                GetReplaceWalletStatusUseCase.Param(args.groupId, args.walletId)
            ).onSuccess { status ->
                val verifiedSigners = status.signers.values
                    .filter { signer -> signer.verifyType != VerifyType.NONE }
                    .mapNotNull { signer -> signer.xfp }
                    .toSet()
                val replaceSignersMap = status.replacements.mapValues { (xfp, replacements) ->
                    replacements.map { signerServer ->
                        signerServer.toModel(
                            getIndexFromPathUseCase(signerServer.derivationPath.orEmpty())
                                .getOrDefault(0)
                        )
                    }
                }
                val nfcMissingBackupKeys = arrayListOf<AddReplaceKeyOnChainData>()

                // Update _keys with replacement signers
                _keys.update { currentKeys ->
                    currentKeys.map { keyData ->
                        var newKeyData: AddReplaceKeyOnChainData
                        val replaceSigners = replaceSignersMap[keyData.fingerPrint]
                        if (replaceSigners != null && replaceSigners.isNotEmpty()) {
                            // Get the original SignerServer objects to access their index
                            val originalReplacements = status.replacements[keyData.fingerPrint] ?: emptyList()
                            val newStepDataMap = replaceSigners.mapIndexed { index, signerModel ->
                                val signerServer = originalReplacements.getOrNull(index)
                                val step = signerServer?.index?.toOnChainReplaceKeyStep(
                                    isGroupWallet = args.groupId.isNotEmpty()
                                ) ?: keyData.type
                                step to ReplaceStepData(
                                    signer = signerModel,
                                    verifyType = signerServer?.verifyType ?: VerifyType.NONE
                                )
                            }.toMap()
                            val mergedStepDataMap = keyData.stepDataMap.toMutableMap().apply {
                                putAll(newStepDataMap)
                            }
                            newKeyData = keyData.copy(stepDataMap = mergedStepDataMap)
                            if (newKeyData.replaceSigners?.firstOrNull()?.type == SignerType.NFC) {
                                if (originalReplacements.any { it.userBackUpFileName.isNullOrEmpty() }) {
                                    nfcMissingBackupKeys.add(newKeyData)
                                }
                            }
                        } else {
                            newKeyData = if (keyData.type == OnChainReplaceKeyStep.TIMELOCK && status.timelock != null) {
                                keyData.updateTimelock(newTimelock = status.timelock!!)
                            } else {
                                keyData.copy(stepDataMap = emptyMap(), newTimelock = null)
                            }
                        }
                        newKeyData
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        missingBackupKeys = nfcMissingBackupKeys,
                        verifiedSigners = verifiedSigners,
                        pendingReplaceXfps = status.pendingReplaceXfps,
                    )
                }
            }.onFailure {
                if (it !is CancellationException) {
                    _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
                }
            }
            _uiState.update { state -> state.copy(isLoading = false, isDataLoaded = true) }
        }
    }

    fun onCreateWallet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            finalizeReplaceAssistedWallet()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun finalizeReplaceAssistedWallet() {
        finalizeReplaceKeyUseCase(
            FinalizeReplaceKeyUseCase.Param(
                groupId = args.groupId,
                walletId = args.walletId
            )
        ).onSuccess { result ->
            if (args.groupId.isEmpty()) {
                getServerWalletsUseCase(Unit)
            } else {
                syncGroupWalletsUseCase(Unit)
            }
            if (result.requiresRegistration) {
                _event.emit(OnChainReplaceKeyEvent.OpenUploadConfigurationScreen(result.wallet.id))
            } else {
                _uiState.update { it.copy(createWalletSuccess = StateEvent.String(result.wallet.id)) }
            }
        }.onFailure {
            _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
        }
    }

    private fun currentUserRole(members: List<ByzantineMember>): AssistedWalletRole {
        return members.firstOrNull {
            it.emailOrUsername == accountManager.getAccount().email
                    || it.emailOrUsername == accountManager.getAccount().username
        }?.role.toRole
    }

    fun setReplacingXfp(xfp: String) {
        viewModelScope.launch {
            savedStateHandle[REPLACE_XFP] = xfp
        }
    }

    fun initReplaceKey() {
        viewModelScope.launch {
            initReplaceKeyUseCase(
                InitReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
                )
            )
        }
    }

    fun markOnCreateWalletSuccess() {
        _uiState.update { it.copy(createWalletSuccess = StateEvent.None) }
    }

    fun onReplaceKey(signer: SingleSigner) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            replaceKeyUseCase(
                ReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty(),
                    signer = signer,
                    keyIndex = getCurrentStep()?.toIndex(args.groupId.isNotEmpty()) ?: 0
                )
            ).onSuccess {
                loadWalletStatusJob?.cancel()
                getReplaceWalletStatus()
            }.onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onHandledMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun onCancelReplaceWallet() {
        viewModelScope.launch {
            resetReplaceKeyUseCase(
                ResetReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId
                )
            ).onSuccess {
                getReplaceWalletStatus()
            }.onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
        }
    }

    val replacedXfp: String
        get() = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()

    fun isEnableContinueButton(): Boolean {
        val keys = _keys.value
        val hasReplacement = keys.any { it.stepDataMap.isNotEmpty() || it.newTimelock != null }
        val allStepsComplete = keys.all { data ->
            data.stepDataMap.isEmpty() || data.steps.all { data.stepDataMap[it]?.isComplete == true }
        }
        val needsVerification = { data: AddReplaceKeyOnChainData ->
            data.type.isAddInheritanceKey || data.replaceSigners?.any { it.type == SignerType.NFC } == true
        }
        val allVerified = keys.filter(needsVerification).all { 
            it.stepDataMap.isEmpty() || it.verifyType != VerifyType.NONE 
        }
        return hasReplacement && allStepsComplete && allVerified
    }

    fun onRemoveKey(xfp: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            removeKeyReplacementUseCase(
                RemoveKeyReplacementUseCase.Params(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = xfp
                )
            ).onSuccess {
                // Remove stepDataMap entries for the removed xfp
                _keys.update { currentKeys ->
                    currentKeys.map { keyData ->
                        if (keyData.fingerPrint == xfp) {
                            keyData.copy(stepDataMap = emptyMap())
                        } else {
                            keyData
                        }
                    }
                }
                // Refresh the wallet status to sync with server
                loadWalletStatusJob?.cancel()
                getReplaceWalletStatus()
            }.onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getRole(): AssistedWalletRole {
        return _uiState.value.myRole
    }

    fun setCurrentStep(step: OnChainReplaceKeyStep) {
        savedStateHandle[KEY_CURRENT_STEP] = step
    }

    fun getCurrentStep(): OnChainReplaceKeyStep? {
        return savedStateHandle.get<OnChainReplaceKeyStep>(KEY_CURRENT_STEP)
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    val result =
                        updateRemoteSignerUseCase.execute(singleSigner.copy(tags = listOf(tag)))
                    if (result is Result.Success) {
                        loadSigners()
                        _event.emit(
                            OnChainReplaceKeyEvent.UpdateSignerTag(
                                signer.copy(
                                    tags = listOf(tag)
                                )
                            )
                        )
                    } else {
                        _event.emit(OnChainReplaceKeyEvent.ShowError((result as Result.Error).exception.message.orUnknownError()))
                    }
                }
        }
    }

    fun onAddKeyClicked(data: AddReplaceKeyOnChainData) {
        viewModelScope.launch {
            // Get the next step that needs to be added
            val nextStep = data.getNextStepToAdd()
            if (nextStep != null) {
                savedStateHandle[KEY_CURRENT_STEP] = nextStep
                _event.emit(OnChainReplaceKeyEvent.OnAddKey(data))
            }
        }
    }

    private fun updateCardForStep(
        step: OnChainReplaceKeyStep,
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

    fun onVerifyClicked(data: AddReplaceKeyOnChainData) {
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
                OnChainReplaceKeyEvent.OnVerifySigner(
                    signer = signer,
                    filePath = nfcFileManager.buildFilePath(stepInfo.keyIdInServer),
                    backUpFileName = getBackUpFileName(stepInfo.extraData),
                    isBackupNfc = _uiState.value.missingBackupKeys.contains(data) && signer.type == SignerType.NFC
                )
            )
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(OnChainReplaceKeyEvent.OnAddAllKey)
        }
    }

    fun requestCacheTapSignerXpub() {
        _uiState.update { it.copy(requestCacheTapSignerXpubEvent = true) }
    }

    fun resetRequestCacheTapSignerXpub() {
        _uiState.update { it.copy(requestCacheTapSignerXpubEvent = false) }
    }

    fun cacheTapSignerXpub(isoDep: IsoDep?, cvc: String) {
        val masterSignerId = savedStateHandle.get<String>(KEY_TAPSIGNER_MASTER_ID) ?: return
        val path = savedStateHandle.get<String>(KEY_TAPSIGNER_PATH) ?: return
        val contextName = savedStateHandle.get<String>(KEY_TAPSIGNER_CONTEXT) ?: return

        isoDep ?: return

        viewModelScope.launch {
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
                        // Resume addTapSignerKey flow
                        processTapSignerWithCompleteData(
                            newSigner,
                            pendingTapSignerSignerModel ?: newSignerModel,
                            pendingTapSignerData,
                            pendingTapSignerWalletId
                        )
                    }

                    TapSignerCachingContext.HANDLE_ACCT1_ADDITION -> {
                        // Resume handleTapSignerAcct1Addition flow
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
                _event.emit(OnChainReplaceKeyEvent.ShowError(error.message.orUnknownError()))
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
        data: AddReplaceKeyOnChainData? = null,
        walletId: String? = null
    ) {
        viewModelScope.launch {
            if (signerModel.isMasterSigner && signerModel.type == SignerType.NFC) {
                var masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                if (masterSigner == null) {
                    loadSigners()
                    masterSigner = masterSigners.find { it.id == signerModel.fingerPrint }
                }
                if (masterSigner == null) {
                    _event.emit(OnChainReplaceKeyEvent.ShowError("Master signer not found with id=${signerModel.fingerPrint}"))
                    return@launch
                }

                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        AddressType.NATIVE_SEGWIT
                    )
                ).onSuccess { singleSigner ->
                    processTapSignerWithCompleteData(singleSigner, signerModel, data, walletId)
                }.onFailure { error ->
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = signerModel.fingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(0, isTestNet)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.ADD_TAPSIGNER_KEY.name
                        pendingTapSignerData = data
                        pendingTapSignerWalletId = walletId
                        pendingTapSignerSignerModel = signerModel
                        requestCacheTapSignerXpub()
                    } else {
                        _event.emit(OnChainReplaceKeyEvent.ShowError(error.message.orUnknownError()))
                    }
                }
            } else {
                _event.emit(OnChainReplaceKeyEvent.ShowError("Invalid signer type for TapSigner addition"))
            }
        }
    }

    private suspend fun processTapSignerWithCompleteData(
        signer: SingleSigner,
        signerModel: SignerModel,
        data: AddReplaceKeyOnChainData? = null,
        walletId: String? = null
    ) {
        val currentStep = getCurrentStep()
            ?: throw IllegalArgumentException("Current step empty")

        onReplaceKey(signer)
        val verifyType = getVerificationTypeForSigner(signerModel, data)

        // Update the card with the new signer for the current step
        updateCardForStep(currentStep, signerModel, verifyType)

        // After successfully adding signer, handle TapSigner Acct 1 addition if we have the required data
        val nextStep = data?.getNextStepToAdd(currentStep)
        if (nextStep != null && walletId != null) {
            setCurrentStep(nextStep)
            handleTapSignerAcct1Addition(data, signerModel, walletId)
        }
    }

    fun handleTapSignerAcct1Addition(
        data: AddReplaceKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            // Try to get signer at index 1
            val signerByIndexResult =
                getSignerFromMasterSignerByIndex(firstSigner.fingerPrint, 1)

            when (signerByIndexResult) {
                is Result.Success -> {
                    // Successfully got signer at index 1, process it with complete data
                    signerByIndexResult.data?.let { singleSigner ->
                        processTapSignerWithCompleteData(
                            singleSigner,
                            singleSigner.toModel(),
                            data
                        )
                    }
                }

                is Result.Error -> {
                    val error = signerByIndexResult.exception
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        // Store context for TapSigner caching - using index 1 for second account
                        savedStateHandle[KEY_TAPSIGNER_MASTER_ID] =
                            firstSigner.fingerPrint
                        savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(1, isTestNet)
                        savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                            TapSignerCachingContext.HANDLE_ACCT1_ADDITION.name
                        pendingTapSignerData = data
                        pendingTapSignerWalletId = walletId
                        pendingTapSignerSignerModel = firstSigner
                        requestCacheTapSignerXpub()
                    } else {
                        // Other error, show error message
                        _event.emit(
                            OnChainReplaceKeyEvent.ShowError(
                                error.message ?: "Failed to get signer at index 1"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getVerificationTypeForSigner(signer: SignerModel, data: AddReplaceKeyOnChainData?): VerifyType {
        return if (data?.isInheritanceKey() == true || signer.type == SignerType.NFC) {
            VerifyType.NONE
        } else {
            VerifyType.APP_VERIFIED
        }
    }

    fun handleSignerNewIndex(signer: SingleSigner, keyOnChainData: AddReplaceKeyOnChainData? = null) {
        viewModelScope.launch {
            val currentStep = getCurrentStep()
                ?: throw IllegalArgumentException("Current step empty")

            onReplaceKey(signer)
            val verifyType = getVerificationTypeForSigner(signer.toModel(), keyOnChainData)

            // Update the card with the new signer for the current step
            updateCardForStep(currentStep, signer.toModel(), verifyType)
        }
    }

    private fun getStepInfo(step: OnChainReplaceKeyStep): MembershipStepInfo {
        // For replace key flow, return a stub MembershipStepInfo
        // Note: MembershipStepInfo uses MembershipStep, but we're using OnChainReplaceKeyStep
        // This is a workaround - the step will need to be converted if needed
        return MembershipStepInfo(
            step = MembershipStep.TIMELOCK, // Default step since we can't convert directly
            groupId = args.groupId
        )
    }

    fun getHardwareSigners(tag: SignerTag): List<SignerModel> =
        _uiState.value.signers.filter {
            it.type == SignerType.HARDWARE && it.tags.contains(tag)
        }

    private fun getBackUpFileName(extra: String): String {
        return runCatching {
            gson.fromJson(extra, SignerExtra::class.java).userKeyFileName
        }.getOrDefault("")
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
            val derivationPath = getPath(index, isTestNet)
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
     * @param data The current AddReplaceKeyOnChainData
     * @param firstSigner The first signer to check
     * @param walletId The wallet ID for navigation
     */
    fun handleSignerIndexCheck(
        data: AddReplaceKeyOnChainData,
        firstSigner: SignerModel,
        walletId: String
    ) {
        viewModelScope.launch {
            // Call GetRemoteSignerUseCase
            val signerResult =
                getRemoteSignerByIndex(firstSigner.fingerPrint, 1)

            when (signerResult) {
                is Result.Success -> {
                    val signer = signerResult.data

                    if (signer != null) {
                        // If signer != null, add signer to corresponding AddReplaceKeyOnChainData.signers
                        handleSignerNewIndex(signer, data)
                    } else {
                        // If signer == null, run handleSignerTypeLogic flow
                        _event.emit(
                            OnChainReplaceKeyEvent.HandleSignerTypeLogic(
                                firstSigner.type,
                                firstSigner.tags.firstOrNull { it != SignerTag.INHERITANCE }
                            )
                        )
                    }
                }

                is Result.Error -> {
                    // If error getting signer, run handleSignerTypeLogic flow
                    _event.emit(
                        OnChainReplaceKeyEvent.HandleSignerTypeLogic(
                            firstSigner.type,
                            firstSigner.tags.firstOrNull { it != SignerTag.INHERITANCE }
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

    private enum class TapSignerCachingContext {
        ADD_TAPSIGNER_KEY,
        HANDLE_ACCT1_ADDITION,
        HANDLE_CUSTOM_KEY_ACCOUNT_RESULT
    }

    companion object {
        const val REPLACE_XFP = "REPLACE_XFP"
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_TAPSIGNER_MASTER_ID = "tapsigner_master_id"
        private const val KEY_TAPSIGNER_PATH = "tapsigner_path"
        private const val KEY_TAPSIGNER_CONTEXT = "tapsigner_context"
    }
}

sealed class OnChainReplaceKeyEvent {
    data class OnAddKey(val data: AddReplaceKeyOnChainData) : OnChainReplaceKeyEvent()
    data class OnVerifySigner(
        val signer: SignerModel,
        val filePath: String,
        val backUpFileName: String,
        val isBackupNfc: Boolean = false
    ) : OnChainReplaceKeyEvent()

    data object OnAddAllKey : OnChainReplaceKeyEvent()
    data object SelectAirgapType : OnChainReplaceKeyEvent()
    data class ShowError(val message: String) : OnChainReplaceKeyEvent()
    data class UpdateSignerTag(val signer: SignerModel) : OnChainReplaceKeyEvent()

    data class HandleSignerTypeLogic(val type: SignerType, val tag: SignerTag?) :
        OnChainReplaceKeyEvent()
    
    data class OpenUploadConfigurationScreen(val walletId: String) : OnChainReplaceKeyEvent()
}

data class OnChainReplaceKeysUiState(
    val isDataLoaded: Boolean = false,
    val pendingReplaceXfps: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val walletSigners: List<SignerModel> = emptyList(),
    val verifiedSigners: Set<String> = emptySet(),
    val group: ByzantineGroup? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val createWalletSuccess: StateEvent = StateEvent.None,
    val signers: List<SignerModel> = emptyList(),
    val message: String = "",
    val isMultiSig: Boolean = false,
    val addressType: AddressType = AddressType.NATIVE_SEGWIT,
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val isRefreshing: Boolean = false,
    val missingBackupKeys: List<AddReplaceKeyOnChainData> = emptyList()
)