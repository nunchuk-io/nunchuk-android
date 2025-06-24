package com.nunchuk.android.app.miniscript

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.miniscript.configurewallet.MiniscriptConfigureWallet
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateMiniscriptWalletUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetCurrentIndexFromMasterSignerUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MiniscriptSharedWalletViewModel @Inject constructor(
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val pushEventManager: PushEventManager,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val getCurrentIndexFromMasterSignerUseCase: GetCurrentIndexFromMasterSignerUseCase,
    private val createMiniscriptWalletUseCase: CreateMiniscriptWalletUseCase,
    private val getChainTipUseCase: GetChainTipUseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val accountManager: AccountManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiniscriptSharedWalletState())
    val uiState = _uiState.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()
    private val keyPositionMap = mutableMapOf<String, String>()
    private var currentKeyToAssign: String = ""
    private var oldSigners: Set<String> = emptySet()
    
    // New state variables for handling TapSigner caching during addSignerToState
    private var pendingAddSignerState: PendingAddSignerState? = null

    init {
        loadInfo()
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                .collect {
                    Timber.d("Pushing event: $it")
                    if (currentKeyToAssign.isNotEmpty()) {
                        addSignerToState(it.signer.toModel(), currentKeyToAssign)
                        currentKeyToAssign = ""
                        _uiState.update { state -> state.copy(currentKeyToAssign = "") }
                        loadInfo()
                    }
                }
        }
        viewModelScope.launch {
            getChainTipUseCase(Unit).onSuccess { blockHeight ->
                Timber.tag(TAG).d("MiniscriptSharedWalletViewModel - blockHeight: $blockHeight")
                _uiState.update { it.copy(currentBlockHeight = blockHeight) }
            }.onFailure { error ->
                Timber.tag(TAG).e("MiniscriptSharedWalletViewModel - error: $error")
            }
        }
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    _uiState.update { state -> state.copy(isTestNet = it == Chain.TESTNET) }
                }
        }
    }

    fun init(args: MiniscriptConfigureWallet) {
        _uiState.update {
            it.copy(
                addressType = args.addressType,
                miniscriptTemplate = args.template,
                walletName = args.walletName,
            )
        }
        getScriptNodeFromTemplate(args.template)
        getSupportedSigners(args.addressType)
    }

    fun setCurrentKeyToAssign(keyName: String) {
        currentKeyToAssign = keyName
        _uiState.update { it.copy(currentKeyToAssign = keyName) }
    }

    private fun loadInfo() {
        viewModelScope.launch {
            getAllSignersUseCase(false).onSuccess { pair ->
                val singleSigner = pair.second.filter { it.type != SignerType.SERVER }
                singleSigners.apply {
                    clear()
                    addAll(singleSigner)
                }
                masterSigners.apply {
                    clear()
                    addAll(pair.first)
                }
                val signers = pair.first.filter { it.type != SignerType.SERVER }
                    .map { signer ->
                        masterSignerMapper(signer)
                    } + singleSigner.map { signer -> signer.toModel() }
                _uiState.update { it.copy(allSigners = signers) }

                Timber.tag(TAG).d("Loaded signers: $signers")
                // Store current signers' fingerprints for comparison later
                oldSigners = signers.map { it.fingerPrint }.toSet()
            }
        }
    }

    fun checkForNewlyAddedSigner() {
        if (currentKeyToAssign.isEmpty()) return

        viewModelScope.launch {
            getAllSignersUseCase(false).onSuccess { pair ->
                val currentSigners = pair.first.filter { it.type != SignerType.SERVER }
                    .map { masterSignerMapper(it) }
                    .plus(pair.second.filter { it.type != SignerType.SERVER }.map { it.toModel() })
                val newSigners = currentSigners.filter { !oldSigners.contains(it.fingerPrint) }
                if (newSigners.isNotEmpty()) {
                    addSignerToState(newSigners.first(), currentKeyToAssign)
                    currentKeyToAssign = ""
                    _uiState.update { it.copy(currentKeyToAssign = "") }
                }
                // Update oldSigners with current state after processing
                oldSigners = currentSigners.map { it.fingerPrint }.toSet()
            }
        }
    }

    private fun getSupportedSigners(type: AddressType) {
        viewModelScope.launch {
            _uiState.mapNotNull { type }.distinctUntilChanged()
                .collect { addressType ->
                    if (addressType.isTaproot() && _uiState.value.supportedTypes.isEmpty()) {
                        getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
                            _uiState.update { it.copy(supportedTypes = supportedTypes) }
                        }
                    }
                }
            getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
                _uiState.update { it.copy(supportedTypes = supportedTypes) }
            }
        }
    }

    fun getScriptNodeFromTemplate(template: String) {
        viewModelScope.launch {
            getScriptNodeFromMiniscriptTemplateUseCase(template).onSuccess { result ->
                Timber.tag(TAG).d("MiniscriptSharedWalletViewModel - scriptNode: ${result.scriptNode} - keyPath: ${result.keyPath}")
                _uiState.update {
                    it.copy(
                        scriptNode = result.scriptNode,
                        keyPath = result.keyPath,
                        areAllKeysAssigned = areAllKeysAssigned(result.scriptNode, it.signers)
                    )
                }
                mapKeyPositions(result.scriptNode, "")
            }.onFailure {
                Timber.tag(TAG).e("MiniscriptSharedWalletViewModel - error: $it")
            }
        }
    }

    private fun mapKeyPositions(node: ScriptNode?, prefix: String) {
        node?.let { scriptNode ->
            scriptNode.keys.forEachIndexed { index, key ->
                val position = if (prefix.isEmpty()) "${index + 1}" else "$prefix.${index + 1}"
                keyPositionMap[key] = position
            }
            scriptNode.subs.forEachIndexed { index, subNode ->
                val newPrefix = if (prefix.isEmpty()) "${index + 1}" else "$prefix.${index + 1}"
                mapKeyPositions(subNode, newPrefix)
            }
        }
    }

    fun addExistingSigner(signer: SignerModel, keyName: String) {
        val addressType = _uiState.value.addressType
        _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(true)) }
        viewModelScope.launch {
            if (signer.isMasterSigner) {
                val masterSigner =
                    masterSigners.find { it.id == signer.fingerPrint } ?: return@launch
                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        addressType
                    )
                ).onSuccess { singleSigner ->
                    Timber.d("Unused signer $singleSigner")
                    addSignerToState(singleSigner.toModel(), keyName)
                }.onFailure { error ->
                    Timber.e("Failed to add signer to group $error")
                    _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError())) }
                }
            } else {
                val singleSigner = singleSigners.find {
                    it.masterFingerprint == signer.fingerPrint &&
                            it.derivationPath == signer.derivationPath
                } ?: return@launch
                addSignerToState(singleSigner.toModel(), keyName)
            }
        }
        _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(false)) }
    }

    private fun addSignerToState(signerModel: SignerModel, keyName: String) {
        Timber.tag(TAG).d("Adding signer: $signerModel to key: $keyName")
        
        // Check if this is a taproot signer (keyName matches keyPath)
        if (keyName == _uiState.value.keyPath && _uiState.value.keyPath.isNotEmpty()) {
            Timber.tag(TAG).d("Storing taproot signer: $signerModel for keyPath: $keyName")
            _uiState.update {
                it.copy(
                    taprootSigner = signerModel,
                    event = MiniscriptSharedWalletEvent.SignerAdded(keyName, signerModel)
                )
            }
            return
        }
        
        val currentSigners = _uiState.value.signers.toMutableMap()

        // If keyName matches the pattern key_x_y, we need to handle master signer case
        val components = keyName.split("_")
        if (components.size > 2 && signerModel.isMasterSigner) {
            Timber.tag(TAG).d("Handling master signer case for key: $keyName")
            // Extract the prefix (key_x)
            val prefix = "${components[0]}_${components[1]}"

            // Find all keys in scriptNode that share the same prefix
            val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                .filter { it.startsWith(prefix) }
                .sorted() // Sort to ensure consistent order

            Timber.tag(TAG).d("Found related keys: $relatedKeys")

            // Get the first available index from the master signer
            viewModelScope.launch {
                getCurrentIndexFromMasterSignerUseCase(
                    GetCurrentIndexFromMasterSignerUseCase.Param(
                        xfp = signerModel.fingerPrint,
                        walletType = WalletType.MULTI_SIG,
                        addressType = _uiState.value.addressType
                    )
                ).onSuccess { startIndex ->
                    Timber.tag(TAG).d("Got start index: $startIndex for master signer: ${signerModel.fingerPrint}")
                    // Reset startIndex to 0 if it's -1
                    val actualStartIndex = if (startIndex == -1) 0 else startIndex

                    // For each related key, get a signer with increasing index
                    var currentIndex = actualStartIndex
                    relatedKeys.forEach { key ->
                        // Only assign a signer if the key hasn't been assigned yet
                        if (currentSigners[key] == null) {
                            Timber.tag(TAG).d("Getting signer at index: $currentIndex for unassigned key: $key")
                            Timber.tag(TAG).d("Using master signer: $signerModel for key: $key")
                            getSignerFromMasterSignerByIndexUseCase(
                                GetSignerFromMasterSignerByIndexUseCase.Param(
                                    masterSignerId = signerModel.fingerPrint,
                                    index = currentIndex,
                                    walletType = WalletType.MULTI_SIG,
                                    addressType = _uiState.value.addressType
                                )
                            ).onSuccess { singleSigner ->
                                singleSigner?.let {
                                    // Add the signer to the currentSigners map
                                    currentSigners[key] = it.toModel()
                                    Timber.tag(TAG).d("Added signer: ${it.toModel()} to key: $key")

                                    // Update UI state immediately
                                    _uiState.update { state ->
                                        state.copy(
                                            signers = currentSigners.toMap(),
                                            areAllKeysAssigned = areAllKeysAssigned(state.scriptNode, currentSigners)
                                        )
                                    }
                                } ?: run {
                                    Timber.tag(TAG).e("No signer found for key: $key at index: $currentIndex")
                                }
                            }.onFailure { error ->
                                Timber.tag(TAG).e("Failed to get signer for key: $key at index: $currentIndex, error: $error")

                                // Handle TapSigner caching case
                                if (error is NCNativeException && error.message.contains("-1009")) {
                                    Timber.tag(TAG).d("Handling TapSigner caching for key: $key at index: $currentIndex")

                                    val isMultisig = isMultisigDerivationPath(signerModel.derivationPath)
                                    val newPath = getPath(currentIndex, _uiState.value.isTestNet, isMultisig)

                                    // Store the pending state
                                    pendingAddSignerState = PendingAddSignerState(
                                        signerModel = signerModel,
                                        keyName = keyName,
                                        relatedKeys = relatedKeys,
                                        currentIndex = currentIndex,
                                        processedKeyIndex = relatedKeys.indexOf(key)
                                    )

                                    // Store the path and signer for caching
                                    savedStateHandle[NEW_PATH] = newPath
                                    _uiState.update {
                                        it.copy(
                                            currentKey = key,
                                            currentSigner = signerModel,
                                            requestCacheTapSignerXpubEvent = true
                                        )
                                    }
                                    return@launch
                                }
                            }
                            currentIndex++
                        } else {
                            Timber.tag(TAG).d("Skipping already assigned key: $key with signer: ${currentSigners[key]}")
                        }
                    }

                    _uiState.update {
                        it.copy(
                            signers = currentSigners,
                            event = MiniscriptSharedWalletEvent.SignerAdded(keyName, signerModel),
                            areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
                        )
                    }
                    Timber.tag(TAG).d("Updated state with new signers: $currentSigners")
                }
            }
        } else {
            Timber.tag(TAG).d("Handling non-master signer case or old key format")
            // Handle non-master signer case or old key format
            currentSigners[keyName] = signerModel

            _uiState.update {
                it.copy(
                    signers = currentSigners,
                    event = MiniscriptSharedWalletEvent.SignerAdded(keyName, signerModel),
                    areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
                )
            }
            Timber.tag(TAG).d("Updated state with new signer: $signerModel for key: $keyName")
        }
    }

    fun removeSigner(keyName: String) {
        Timber.tag(TAG).d("Removing signer for key: $keyName")
        
        // Check if this is a taproot signer removal
        if (keyName == _uiState.value.keyPath && _uiState.value.keyPath.isNotEmpty()) {
            Timber.tag(TAG).d("Removing taproot signer for keyPath: $keyName")
            _uiState.update {
                it.copy(
                    taprootSigner = null,
                    event = MiniscriptSharedWalletEvent.SignerRemoved(keyName)
                )
            }
            return
        }
        
        val currentSigners = _uiState.value.signers.toMutableMap()

        // If keyName matches the pattern key_x_y, we need to handle related keys
        val components = keyName.split("_")
        if (components.size > 2) {
            Timber.tag(TAG).d("Handling key with pattern key_x_y")
            // Extract the prefix (key_x)
            val prefix = "${components[0]}_${components[1]}"

            // Get the signer to remove
            val signerToRemove = currentSigners[keyName]
            Timber.tag(TAG).d("Signer to remove: $signerToRemove")

            // If it's a master signer, remove all related keys with the same master fingerprint
            if (signerToRemove?.isMasterSigner == true) {
                val masterFingerprintToRemove = signerToRemove.fingerPrint
                Timber.tag(TAG).d("Removing all related keys with master fingerprint: $masterFingerprintToRemove")

                // Find all keys in scriptNode that share the same prefix
                val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                    .filter { it.startsWith(prefix) }

                Timber.tag(TAG).d("Found related keys to remove: $relatedKeys")

                // Remove signers for all related keys with the same master fingerprint
                relatedKeys.forEach { key ->
                    if (currentSigners[key]?.fingerPrint == masterFingerprintToRemove) {
                        currentSigners[key] = null
                        Timber.tag(TAG).d("Removed signer for key: $key")
                    }
                }
            } else {
                // For non-master signers, just remove the specific key
                currentSigners[keyName] = null
                Timber.tag(TAG).d("Removed non-master signer for key: $keyName")
            }
        } else {
            // Handle old key format
            currentSigners[keyName] = null
            Timber.tag(TAG).d("Removed signer for old format key: $keyName")
        }

        _uiState.update {
            it.copy(
                signers = currentSigners,
                event = MiniscriptSharedWalletEvent.SignerRemoved(keyName),
                areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
            )
        }
        Timber.tag(TAG).d("Updated state after removing signer(s). Current signers: $currentSigners")
    }

    private fun areAllKeysAssigned(scriptNode: ScriptNode?, signers: Map<String, SignerModel?>): Boolean {
        if (scriptNode == null) return false
        val allKeys = getAllKeysFromScriptNode(scriptNode)
        val areScriptKeysAssigned = allKeys.all { keyName -> signers[keyName] != null }
        
        // Also check if taproot signer is assigned when keyPath is present
        val keyPath = _uiState.value.keyPath
        val isTaprootKeyAssigned = if (keyPath.isNotEmpty()) {
            _uiState.value.taprootSigner != null
        } else {
            true // No taproot key required
        }
        
        return areScriptKeysAssigned && isTaprootKeyAssigned
    }

    private fun getAllKeysFromScriptNode(node: ScriptNode): Set<String> {
        val keys = node.keys.toMutableSet()
        node.subs.forEach { subNode ->
            keys.addAll(getAllKeysFromScriptNode(subNode))
        }
        return keys
    }

    fun getSuggestedSigners(): List<SupportedSigner> {
        return _uiState.value.let { state ->
            state.supportedTypes.takeIf { state.addressType.isTaproot() == true }.orEmpty()
        }
    }

    fun getKeyPosition(keyName: String): String {
        return keyPositionMap[keyName] ?: ""
    }

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }

    fun changeBip32Path(keyName: String, signer: SignerModel) {
        _uiState.update {
            it.copy(
                currentKey = keyName,
                currentSigner = signer,
                event = MiniscriptSharedWalletEvent.OpenChangeBip32Path(keyName, signer)
            )
        }
    }

    fun updateBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(true)) }
            val currentKey = _uiState.value.currentKey
            if (currentKey.isEmpty()) {
                _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(false)) }
                return@launch
            }
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId, newPath
                )
            ).onSuccess { newSigner ->
                val newSignerModel = newSigner.toModel()
                
                // Check if this is updating a taproot signer
                if (currentKey == _uiState.value.keyPath && _uiState.value.keyPath.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            taprootSigner = newSignerModel,
                            event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSignerModel),
                            currentKey = ""
                        )
                    }
                } else {
                    // Update regular script signer
                    val currentSigners = _uiState.value.signers.toMutableMap()
                    currentSigners[currentKey] = newSignerModel
                    _uiState.update {
                        it.copy(
                            signers = currentSigners,
                            event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSignerModel),
                            currentKey = ""
                        )
                    }
                }
            }.onFailure { error ->
                if (error is NCNativeException && error.message.contains("-1009")) {
                    savedStateHandle[NEW_PATH] = newPath
                    _uiState.update { it.copy(requestCacheTapSignerXpubEvent = true) }
                } else {
                    Timber.e("Failed to change bip32 path $error")
                    _uiState.update {
                        it.copy(
                            event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                            currentKey = ""
                        )
                    }
                }
            }
        }
    }

    fun getKeySignerMap(): Map<String, SingleSigner> {
        val result = mutableMapOf<String, SingleSigner>()
        val scriptNode = _uiState.value.scriptNode ?: return result
        val signers = _uiState.value.signers

        fun traverseNode(node: ScriptNode) {
            // Process keys in current node
            node.keys.forEach { key ->
                signers[key]?.let { signerModel ->
                    result[key] = signerModel.toSingleSigner()
                }
            }

            // Recursively process sub-nodes
            node.subs.forEach { subNode ->
                traverseNode(subNode)
            }
        }

        traverseNode(scriptNode)
        
        // Add taproot signer if available
        val keyPath = _uiState.value.keyPath
        val taprootSigner = _uiState.value.taprootSigner
        if (keyPath.isNotEmpty() && taprootSigner != null) {
            Timber.tag(TAG).d("Adding taproot signer to keySignerMap: $taprootSigner for keyPath: $keyPath")
            result[keyPath] = taprootSigner.toSingleSigner()
        }
        
        return result
    }

    fun createMiniscriptWallet() {
        val signers = _uiState.value.signers.values.filterNotNull()

        if (signers.isEmpty()) {
            _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Error("No signers assigned.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(true)) }
            createMiniscriptWalletUseCase(
                CreateMiniscriptWalletUseCase.Params(
                    miniscriptTemplate = _uiState.value.miniscriptTemplate,
                    signerMap = getKeySignerMap(),
                    name = _uiState.value.walletName,
                    description = "",
                    addressType = _uiState.value.addressType.ordinal,
                    allowUsedSigner = true,
                    decoyPin = accountManager.getAccount().decoyPin
                )
            ).onSuccess { wallet ->
                Timber.tag(TAG).d("About to emit CreateWalletSuccess event with wallet: $wallet")
                _uiState.update {
                    it.copy(
                        event = MiniscriptSharedWalletEvent.CreateWalletSuccess(wallet)
                    )
                }
                Timber.tag(TAG).d("CreateWalletSuccess event emitted")
            }.onFailure { error ->
                Timber.e("Failed to create miniscript wallet: $error")
                _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError())) }
            }
        }
    }

    fun resetRequestCacheTapSignerXpub() {
        _uiState.update { it.copy(requestCacheTapSignerXpubEvent = false) }
    }

    fun cacheTapSignerXpub(isoDep: IsoDep?, cvc: String) {
        val signer = _uiState.value.currentSigner ?: return
        val newPath = savedStateHandle.get<String>(NEW_PATH) ?: return
        Timber.tag(TAG).d("Caching tap signer xpub $signer with path: $newPath")
        isoDep ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Loading(true)) }
            getSignerFromTapsignerMasterSignerByPathUseCase(
                GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                    isoDep = isoDep,
                    masterSignerId = signer.id,
                    path = newPath,
                    cvc = cvc
                )
            ).onSuccess { newSigner ->
                Timber.tag(TAG).d("new signer $newSigner")
                val currentKey = _uiState.value.currentKey
                if (currentKey.isNotEmpty()) {
                    val newSignerModel = newSigner.toModel()
                    
                    // Check if this is updating a taproot signer
                    if (currentKey == _uiState.value.keyPath && _uiState.value.keyPath.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                taprootSigner = newSignerModel,
                                event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSignerModel),
                                currentKey = "",
                                requestCacheTapSignerXpubEvent = false
                            )
                        }
                    } else {
                        // Update regular script signer
                        val currentSigners = _uiState.value.signers.toMutableMap()
                        currentSigners[currentKey] = newSignerModel
                        _uiState.update {
                            it.copy(
                                signers = currentSigners,
                                event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSignerModel),
                                currentKey = "",
                                requestCacheTapSignerXpubEvent = false
                            )
                        }
                    }
                }

                // If we have a pending addSignerToState process, resume it
                if (pendingAddSignerState != null) {
                    resumeAddSignerProcess()
                }
            }.onFailure { error ->
                Timber.tag(TAG).e("Failed to cache tap signer xpub: $error")
                _uiState.update {
                    it.copy(
                        event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                        requestCacheTapSignerXpubEvent = false
                    )
                }
                // Clear pending state on failure
                pendingAddSignerState = null
            }
        }
    }

    private fun isMultisigDerivationPath(derivationPath: String): Boolean {
        return derivationPath.contains("48h") || derivationPath.contains("48'")
    }

    private fun getPath(index: Int, isTestNet: Boolean, isMultisig: Boolean): String {
        if (isMultisig) {
            return if (isTestNet) "m/48h/1h/${index}h/2h" else "m/48h/0h/${index}h/2h"
        }
        return if (isTestNet) "m/84h/1h/${index}h" else "m/84h/0h/${index}h"
    }

    private fun resumeAddSignerProcess() {
        val pendingState = pendingAddSignerState ?: return
        pendingAddSignerState = null

        Timber.tag(TAG).d("Resuming addSignerToState process for key: ${pendingState.keyName}")

        // Resume the process from where it left off
        val currentSigners = _uiState.value.signers.toMutableMap()
        val relatedKeys = pendingState.relatedKeys
        val signerModel = pendingState.signerModel

        viewModelScope.launch {
            // Continue from the index where we left off
            var currentIndex = pendingState.currentIndex
            val startFromIndex = pendingState.processedKeyIndex

            // Process remaining keys starting from where we left off
            for (i in startFromIndex until relatedKeys.size) {
                val key = relatedKeys[i]

                // Only assign a signer if the key hasn't been assigned yet
                if (currentSigners[key] == null) {
                    Timber.tag(TAG).d("Resuming - Getting signer at index: $currentIndex for key: $key")

                    getSignerFromMasterSignerByIndexUseCase(
                        GetSignerFromMasterSignerByIndexUseCase.Param(
                            masterSignerId = signerModel.fingerPrint,
                            index = currentIndex,
                            walletType = WalletType.MULTI_SIG,
                            addressType = _uiState.value.addressType
                        )
                    ).onSuccess { singleSigner ->
                        singleSigner?.let {
                            currentSigners[key] = it.toModel()
                            Timber.tag(TAG).d("Resumed - Added signer: ${it.toModel()} to key: $key")

                            // Update UI state immediately
                            _uiState.update { state ->
                                state.copy(
                                    signers = currentSigners.toMap(),
                                    areAllKeysAssigned = areAllKeysAssigned(state.scriptNode, currentSigners)
                                )
                            }
                        } ?: run {
                            Timber.tag(TAG).e("Resumed - No signer found for key: $key at index: $currentIndex")
                        }
                    }.onFailure { error ->
                        Timber.tag(TAG).e("Resumed - Failed to get signer for key: $key at index: $currentIndex, error: $error")

                        // Handle nested TapSigner caching case if needed
                        if (error is NCNativeException && error.message.contains("-1009")) {
                            val isMultisig = isMultisigDerivationPath(signerModel.derivationPath)
                            val newPath = getPath(currentIndex, _uiState.value.isTestNet, isMultisig)

                            pendingAddSignerState = PendingAddSignerState(
                                signerModel = signerModel,
                                keyName = pendingState.keyName,
                                relatedKeys = relatedKeys,
                                currentIndex = currentIndex,
                                processedKeyIndex = i
                            )

                            savedStateHandle[NEW_PATH] = newPath
                            _uiState.update {
                                it.copy(
                                    currentKey = key,
                                    currentSigner = signerModel,
                                    requestCacheTapSignerXpubEvent = true
                                )
                            }
                            return@launch
                        }
                    }
                    currentIndex++
                } else {
                    Timber.tag(TAG).d("Resumed - Skipping already assigned key: $key")
                }
            }

            // Update the final state
            _uiState.update {
                it.copy(
                    signers = currentSigners,
                    event = MiniscriptSharedWalletEvent.SignerAdded(pendingState.keyName, signerModel),
                    areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
                )
            }
            Timber.tag(TAG).d("Resumed - Updated state with signers: $currentSigners")
        }
    }

    companion object {
        private const val NEW_PATH = "new_path"
        private const val TAG = "miniscript-feature"
    }
}

data class MiniscriptSharedWalletState(
    val scriptNode: ScriptNode? = null,
    val keyPath: String = "",
    val supportedTypes: List<SupportedSigner> = emptyList(),
    val signers: Map<String, SignerModel?> = emptyMap(),
    val allSigners: List<SignerModel> = emptyList(),
    val addressType: AddressType = AddressType.ANY,
    val event: MiniscriptSharedWalletEvent? = null,
    val areAllKeysAssigned: Boolean = false,
    val currentKey: String = "",
    val currentSigner: SignerModel? = null,
    val miniscriptTemplate: String = "",
    val walletName: String = "",
    val currentBlockHeight: Int = 0,
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val isTestNet: Boolean = false,
    val currentKeyToAssign: String = "",
    val taprootSigner: SignerModel? = null
)

sealed class MiniscriptSharedWalletEvent {
    data class Loading(val isLoading: Boolean) : MiniscriptSharedWalletEvent()
    data class Error(val message: String) : MiniscriptSharedWalletEvent()
    data class SignerAdded(val keyName: String, val signer: SignerModel) :
        MiniscriptSharedWalletEvent()

    data class SignerRemoved(val keyName: String) : MiniscriptSharedWalletEvent()
    data class OpenChangeBip32Path(val keyName: String, val signer: SignerModel) :
        MiniscriptSharedWalletEvent()

    data class Bip32PathChanged(val signer: SignerModel) : MiniscriptSharedWalletEvent()
    data class CreateWalletSuccess(val wallet: Wallet) : MiniscriptSharedWalletEvent()
    data object RequestCacheTapSignerXpub : MiniscriptSharedWalletEvent()
}

data class PendingAddSignerState(
    val signerModel: SignerModel,
    val keyName: String,
    val relatedKeys: List<String>,
    val currentIndex: Int,
    val processedKeyIndex: Int
) 