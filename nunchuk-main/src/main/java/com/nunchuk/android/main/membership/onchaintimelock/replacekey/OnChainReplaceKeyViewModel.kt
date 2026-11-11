package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import android.nfc.tech.IsoDep
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CreateWallet2UseCase
import com.nunchuk.android.core.domain.membership.SyncPersonalWallets
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
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.onchaintimelock.addkey.fallbackNumber
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
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.inheritance.RecoveredKey
import com.nunchuk.android.model.inheritance.bip32AccountIndex
import com.nunchuk.android.model.wallet.ReplaceWalletStatus
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
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
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
import kotlinx.coroutines.flow.getAndUpdate
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
    private val syncPersonalWallets: SyncPersonalWallets,
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
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
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
    private var initReplaceKeyJob: Job? = null

    /** Set once the wallet turns out to be configurable; null for a legacy wallet. */
    private var configurableWallet: WalletServer? = null
    private var lastStatus: ReplaceWalletStatus? = null
    private val _configurable = MutableStateFlow<ConfigurableReplaceState?>(null)
    val configurable = _configurable.asStateFlow()

    /** The configurable slot the running import fills; null on a legacy wallet. */
    val configurableRequest: ConfigurableReplaceRequest?
        get() = savedStateHandle.get<ConfigurableReplaceRequest>(KEY_CONFIGURABLE_REQUEST)

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
                    onWalletLoaded(wallet)
                }
            }
        } else {
            viewModelScope.launch {
                getServerWalletUseCase(args.walletId).onSuccess { wallet ->
                    onWalletLoaded(wallet)
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

    /**
     * A configurable wallet (NUN-10320) is laid out from the replacement status' cards; its key
     * indices have no [OnChainReplaceKeyStep]. A legacy wallet keeps the fixed step layout.
     */
    private suspend fun onWalletLoaded(wallet: WalletServer) {
        if (wallet.onChainInheritancePolicy?.isConfigurable == true) {
            configurableWallet = wallet
            publishConfigurable()
        } else {
            buildAddReplaceKeySteps(wallet.signers, wallet.timelock)
        }
    }

    private fun publishConfigurable() {
        val wallet = configurableWallet ?: return
        val status = lastStatus ?: return
        val state = buildConfigurableReplaceState(
            policy = wallet.onChainInheritancePolicy,
            walletSigners = wallet.signers,
            walletTimelock = wallet.timelock,
            status = status,
        )
        _configurable.value = state
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

    // The server retires the old wallet id on finalize, so a later refresh only 404s.
    fun getReplaceWalletStatus() {
        if (_uiState.value.isFinalizing || _uiState.value.isWalletFinalized) return
        if (loadWalletStatusJob?.isActive == true) return
        loadWalletStatusJob = viewModelScope.launch {
            getReplaceWalletStatusUseCase(
                GetReplaceWalletStatusUseCase.Param(args.groupId, args.walletId)
            ).onSuccess { status ->
                lastStatus = status
                publishConfigurable()
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
            // Don't clear an isLoading this refresh didn't set — it hid another action's dialog.
            _uiState.update { state -> state.copy(isDataLoaded = true) }
        }
    }

    fun onCreateWallet() {
        // A second tap would finalize a wallet the server has already replaced.
        if (_uiState.value.isFinalizing || _uiState.value.isWalletFinalized) return
        viewModelScope.launch {
            loadWalletStatusJob?.cancel()
            _uiState.update { it.copy(isLoading = true, isFinalizing = true) }
            // The backend checks verification at finalize; its error shows as a toast.
            finalizeReplaceAssistedWallet()
            _uiState.update { it.copy(isLoading = false, isFinalizing = false) }
        }
    }

    private suspend fun finalizeReplaceAssistedWallet() {
        finalizeReplaceKeyUseCase(
            FinalizeReplaceKeyUseCase.Param(
                groupId = args.groupId,
                walletId = args.walletId
            )
        ).onSuccess { result ->
            _uiState.update { it.copy(isWalletFinalized = true) }
            if (args.groupId.isEmpty()) {
                syncPersonalWallets(Unit)
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

    // Opens the replacement session; a replace that overtakes it has no session to attach to.
    fun initReplaceKey() {
        initReplaceKeyJob = viewModelScope.launch {
            initReplaceKeyUseCase(
                InitReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
                )
            ).onFailure {
                _uiState.update { state -> state.copy(message = it.message.orUnknownError()) }
            }
        }
    }

    fun markOnCreateWalletSuccess() {
        _uiState.update { it.copy(createWalletSuccess = StateEvent.None) }
    }

    fun onReplaceKey(signer: SingleSigner) {
        val request = configurableRequest
        if (request != null && !acceptsConfigurableSigner(request, signer)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            initReplaceKeyJob?.join()
            replaceKeyUseCase(
                ReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty(),
                    signer = signer,
                    keyIndex = request?.keyIndex ?: getCurrentStep()?.toIndex(args.groupId.isNotEmpty()) ?: 0
                )
            ).onSuccess {
                loadWalletStatusJob?.cancel()
                getReplaceWalletStatus()
                if (request != null) {
                    loadWalletStatusJob?.join()
                    continueConfigurableReplace(request.originalXfp)
                }
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
                savedStateHandle.remove<ConfigurableReplaceRequest>(KEY_CONFIGURABLE_REQUEST)
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
                if (configurableRequest?.originalXfp.equals(xfp, ignoreCase = true)) {
                    savedStateHandle.remove<ConfigurableReplaceRequest>(KEY_CONFIGURABLE_REQUEST)
                }
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
                    updateRemoteSignerUseCase(singleSigner.copy(tags = listOf(tag)))
                        .onSuccess {
                            loadSigners()
                            _event.emit(
                                OnChainReplaceKeyEvent.UpdateSignerTag(
                                    signer.copy(
                                        tags = listOf(tag)
                                    )
                                )
                            )
                        }.onFailure {
                            _event.emit(OnChainReplaceKeyEvent.ShowError(it.message.orUnknownError()))
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
        savedStateHandle[KEY_VERIFY_FALLBACK_NUMBER] = null
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

    /**
     * The replacement key proved which device it lives on — for Ledger and BitBox by reporting the
     * fingerprint back from their own screens, which create nothing and leave the marking to us.
     * Coldcard and air-gap mark themselves verified inside their add-key screens instead.
     */
    fun setReplaceKeyVerified(keyId: String, recoveredKey: RecoveredKey? = null) {
        viewModelScope.launch {
            setReplaceKeyVerifiedUseCase(
                SetReplaceKeyVerifiedUseCase.Param(
                    keyId = keyId,
                    checkSum = "",
                    verifyType = VerifyType.SELF_VERIFIED,
                    groupId = args.groupId,
                    walletId = args.walletId,
                    recoveredKey = recoveredKey,
                )
            ).onSuccess {
                getReplaceWalletStatus()
                _event.emit(OnChainReplaceKeyEvent.OnKeyVerified(fallbackNumber = savedStateHandle[KEY_VERIFY_FALLBACK_NUMBER]))
            }.onFailure {
                _event.emit(OnChainReplaceKeyEvent.ShowError(it.message.orUnknownError()))
            }
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
        if (configurableRequest != null) {
            onConfigurableDevicePicked(signerModel)
            return
        }
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
        val request = configurableRequest
        if (request != null) {
            // A key picked or read at another account: fetch the slot's account of that device.
            if (bip32AccountIndex(signer.derivationPath) == request.accountIndex) {
                onReplaceKey(signer)
            } else {
                onConfigurableDevicePicked(signer.toModel())
            }
            return
        }
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

    /**
     * Replace on a configurable wallet's key (REP-01). Every account the original key supplies, in
     * any card, has to come off one new key: the first from a device the user picks, the rest off
     * that same device without asking again (REP-02).
     */
    fun onConfigurableReplaceClicked(row: ReplaceKeyRow) {
        val state = _configurable.value ?: return
        val target = state.targetOf(row) ?: return
        val slot = target.nextMissingSlot ?: return
        savedStateHandle[REPLACE_XFP] = target.originalXfp
        initReplaceKey()
        // REP-07: a fallback-only key can be replaced by a key already in this wallet.
        val options = if (target.isFallbackOnly && target.replacementDevice == null) state.chooseKeyOptions(target) else emptyList()
        if (options.isNotEmpty()) {
            _chooseWalletKey.value = ChooseWalletKeyPrompt(
                originalXfp = target.originalXfp,
                fallbackNumber = state.fallbackNumberOf(target),
                options = options,
            )
            return
        }
        startConfigurableImport(target, slot, target.replacementDevice)
    }

    private val _chooseWalletKey = MutableStateFlow<ChooseWalletKeyPrompt?>(null)

    /** REP-07 "Choose a key for Fallback N", while it is open. */
    val chooseWalletKey = _chooseWalletKey.asStateFlow()

    /** REP-07 "Choose key": that wallet key joins at its next free account, read off that device. */
    fun onChooseWalletKeySelected(xfp: String) {
        val prompt = _chooseWalletKey.getAndUpdate { null } ?: return
        val target = _configurable.value?.targets?.get(prompt.originalXfp.lowercase()) ?: return
        val slot = target.nextMissingSlot ?: return
        val device = prompt.options.firstOrNull { it.fingerPrint.equals(xfp, ignoreCase = true) } ?: return
        startConfigurableImport(target, slot, device, isWalletKey = true)
    }

    /** REP-07 "Use a different key": the usual device import. */
    fun onChooseWalletKeyAddNew() {
        val prompt = _chooseWalletKey.getAndUpdate { null } ?: return
        val target = _configurable.value?.targets?.get(prompt.originalXfp.lowercase()) ?: return
        val slot = target.nextMissingSlot ?: return
        startConfigurableImport(target, slot, null)
    }

    fun dismissChooseWalletKey() {
        _chooseWalletKey.value = null
    }

    /** After an account is in: import the next missing one off the same device, if any. */
    private fun continueConfigurableReplace(originalXfp: String) {
        val isWalletKey = configurableRequest?.isWalletKey == true
        val target = _configurable.value?.targets?.get(originalXfp.lowercase())
        val slot = target?.nextMissingSlot
        val device = target?.replacementDevice
        if (slot == null || device == null) {
            savedStateHandle.remove<ConfigurableReplaceRequest>(KEY_CONFIGURABLE_REQUEST)
            return
        }
        startConfigurableImport(target, slot, device, isWalletKey)
    }

    /** Accounts each XFP already gives the wallet or its pending replacement. */
    private fun usedAccounts(): List<Pair<String, String>> {
        val wallet = configurableWallet?.signers.orEmpty().map { it.xfp.orEmpty() to it.derivationPath.orEmpty() }
        val pending = lastStatus?.replacements?.values.orEmpty().flatten().map { it.xfp.orEmpty() to it.derivationPath.orEmpty() }
        return wallet + pending
    }

    private fun startConfigurableImport(
        target: ReplaceTarget,
        slot: ReplaceSlot,
        device: SignerModel?,
        isWalletKey: Boolean = false,
    ) {
        val isInheritance = target.slots.any { it.original?.tags?.contains(SignerTag.INHERITANCE) == true }
        val request = ConfigurableReplaceRequest(
            originalXfp = target.originalXfp,
            keyIndex = slot.keyIndex,
            // A wallet key (REP-07) takes Account N+1 of that device, not the old key's account.
            accountIndex = if (isWalletKey && device != null) nextFreeAccount(device.fingerPrint, usedAccounts()) else slot.accountIndex,
            isInheritance = isInheritance,
            sameDevice = device,
            isWalletKey = isWalletKey,
        )
        savedStateHandle[KEY_CONFIGURABLE_REQUEST] = request
        // Stand-in step for the device flows' inheritance/spending copy; the slot index is the request's.
        setCurrentStep(if (isInheritance) OnChainReplaceKeyStep.INHERITANCE_KEY else OnChainReplaceKeyStep.HARDWARE_KEY)
        if (device == null) {
            viewModelScope.launch { _event.emit(OnChainReplaceKeyEvent.OpenConfigurableAdd(request)) }
        } else {
            importConfigurableAccount(device, request)
        }
    }

    /** A device was picked (or read at another account) for the running configurable import. */
    fun onConfigurableDevicePicked(device: SignerModel) {
        val request = configurableRequest ?: return
        if (request.sameDevice != null && !request.sameDevice.fingerPrint.equals(device.fingerPrint, ignoreCase = true)) {
            emitSameDeviceError(request)
            return
        }
        importConfigurableAccount(device, request.copy(sameDevice = device), readFromDevice = request.sameDevice == null)
    }

    /**
     * [request]'s account of [device]: reuse it when the app or the server already holds it (TAPSIGNER:
     * derive it), else read exactly that account off the device. When the device itself just handed
     * back another account ([readFromDevice] false), report the mismatch instead of asking again.
     */
    private fun importConfigurableAccount(
        device: SignerModel,
        request: ConfigurableReplaceRequest,
        readFromDevice: Boolean = true,
    ) {
        savedStateHandle[KEY_CONFIGURABLE_REQUEST] = request
        viewModelScope.launch {
            if (device.type == SignerType.NFC) {
                when (val result = getSignerFromMasterSignerByIndex(device.fingerPrint, request.accountIndex)) {
                    is Result.Success -> result.data?.let { onReplaceKey(it) }
                    is Result.Error -> {
                        val error = result.exception
                        if (error is NCNativeException && error.message.contains("-1009")) {
                            savedStateHandle[KEY_TAPSIGNER_MASTER_ID] = device.fingerPrint
                            savedStateHandle[KEY_TAPSIGNER_PATH] = getPath(request.accountIndex, isTestNet)
                            savedStateHandle[KEY_TAPSIGNER_CONTEXT] =
                                TapSignerCachingContext.HANDLE_CUSTOM_KEY_ACCOUNT_RESULT.name
                            pendingTapSignerSignerModel = device
                            requestCacheTapSignerXpub()
                        } else {
                            _event.emit(OnChainReplaceKeyEvent.ShowError(error.message.orUnknownError()))
                        }
                    }
                }
                return@launch
            }
            val known = getRemoteSignerByIndex(device.fingerPrint, request.accountIndex)
            if (known is Result.Success && known.data != null) {
                onReplaceKey(known.data!!)
                return@launch
            }
            if (readFromDevice) {
                _event.emit(OnChainReplaceKeyEvent.OpenConfigurableAdd(request))
            } else {
                _event.emit(OnChainReplaceKeyEvent.ShowErrorRes(R.string.nc_draft_key_wrong_account, listOf(request.accountIndex)))
            }
        }
    }

    /** The replacement must be [request]'s account, and every account must come off the same key. */
    private fun acceptsConfigurableSigner(request: ConfigurableReplaceRequest, signer: SingleSigner): Boolean {
        if (request.sameDevice != null && !request.sameDevice.fingerPrint.equals(signer.masterFingerprint, ignoreCase = true)) {
            emitSameDeviceError(request)
            return false
        }
        if (bip32AccountIndex(signer.derivationPath) != request.accountIndex) {
            viewModelScope.launch {
                _event.emit(OnChainReplaceKeyEvent.ShowErrorRes(R.string.nc_draft_key_wrong_account, listOf(request.accountIndex)))
            }
            return false
        }
        return true
    }

    private fun emitSameDeviceError(request: ConfigurableReplaceRequest) {
        viewModelScope.launch {
            _event.emit(
                OnChainReplaceKeyEvent.ShowErrorRes(
                    R.string.nc_replace_key_same_device,
                    listOf(request.accountIndex, request.sameDevice?.fingerPrint.orEmpty().uppercase()),
                )
            )
        }
    }

    /** Verify once the new key has every account (REP-03); one check covers all of them. */
    fun onConfigurableVerifyClicked(row: ReplaceKeyRow) {
        val target = _configurable.value?.targetOf(row)?.takeIf { it.isComplete } ?: return
        val signer = target.replacementDevice ?: return
        savedStateHandle[REPLACE_XFP] = target.originalXfp
        // Verify tapped on a fallback card: the backup screens name "Fallback N key" (FBK-13).
        val fallbackNumber = _configurable.value?.cards?.firstOrNull { row in it.rows }
            ?.card?.fallbackNumber?.takeIf { it > 0 }
        savedStateHandle[KEY_VERIFY_FALLBACK_NUMBER] = fallbackNumber
        val missingBackup = signer.type == SignerType.NFC &&
            lastStatus?.replacements?.get(target.originalXfp)?.any { it.userBackUpFileName.isNullOrEmpty() } == true
        viewModelScope.launch {
            _event.emit(
                OnChainReplaceKeyEvent.OnVerifySigner(
                    signer = signer,
                    filePath = nfcFileManager.buildFilePath(getStepInfo(OnChainReplaceKeyStep.HARDWARE_KEY).keyIdInServer),
                    backUpFileName = "",
                    isBackupNfc = missingBackup,
                    fallbackNumber = fallbackNumber,
                )
            )
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
        private const val KEY_CONFIGURABLE_REQUEST = "configurable_replace_request"
        private const val KEY_VERIFY_FALLBACK_NUMBER = "verify_fallback_number"
    }
}

sealed class OnChainReplaceKeyEvent {
    data class OnAddKey(val data: AddReplaceKeyOnChainData) : OnChainReplaceKeyEvent()
    data class OnVerifySigner(
        val signer: SignerModel,
        val filePath: String,
        val backUpFileName: String,
        val isBackupNfc: Boolean = false,
        /** 1-based, when verifying from a fallback card. */
        val fallbackNumber: Int? = null,
    ) : OnChainReplaceKeyEvent()

    data object OnAddAllKey : OnChainReplaceKeyEvent()
    data class OnKeyVerified(val fallbackNumber: Int? = null) : OnChainReplaceKeyEvent()
    data object SelectAirgapType : OnChainReplaceKeyEvent()
    data class ShowError(val message: String) : OnChainReplaceKeyEvent()
    data class UpdateSignerTag(val signer: SignerModel) : OnChainReplaceKeyEvent()

    data class HandleSignerTypeLogic(val type: SignerType, val tag: SignerTag?) :
        OnChainReplaceKeyEvent()
    
    data class OpenUploadConfigurationScreen(val walletId: String) : OnChainReplaceKeyEvent()

    /** Run the add-key flow for a configurable wallet slot ([ConfigurableReplaceRequest.sameDevice]: that device only). */
    data class OpenConfigurableAdd(val request: ConfigurableReplaceRequest) : OnChainReplaceKeyEvent()
    data class ShowErrorRes(@StringRes val messageRes: Int, val args: List<Any> = emptyList()) : OnChainReplaceKeyEvent()
}

/** REP-07: replace the fallback-only key [originalXfp] with one of [options], keys already in this wallet. */
data class ChooseWalletKeyPrompt(
    val originalXfp: String,
    val fallbackNumber: Int,
    val options: List<SignerModel>,
)

data class OnChainReplaceKeysUiState(
    val isDataLoaded: Boolean = false,
    val pendingReplaceXfps: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isFinalizing: Boolean = false,
    val isWalletFinalized: Boolean = false,
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