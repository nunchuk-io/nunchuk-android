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
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.ScriptNodeType
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
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.ClearSignerPassphraseUseCase
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
    private val singleSignerMapper: SingleSignerMapper,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val accountManager: AccountManager,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    private val clearSignerPassphraseUseCase: ClearSignerPassphraseUseCase,
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
                reuseSigner = args.reuseSigner,
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
                
                // Create scriptNodeMuSig if keyPath.size > 1
                val scriptNodeMuSig = if (result.keyPath.size > 1) {
                    ScriptNode(
                        id = listOf(1),
                        type = ScriptNodeType.MUSIG.name,
                        keys = result.keyPath,
                        subs = emptyList(),
                        k = 0,
                        data = byteArrayOf(),
                        timeLock = null
                    )
                } else null
                
                _uiState.update {
                    it.copy(
                        scriptNode = result.scriptNode,
                        keyPaths = result.keyPath,
                        scriptNodeMuSig = scriptNodeMuSig,
                        areAllKeysAssigned = areAllKeysAssigned(result.scriptNode, it.signers)
                    )
                }
                mapKeyPositions(result.scriptNode, "")
                // Also map keys from scriptNodeMuSig if it exists
                scriptNodeMuSig?.let { mapKeyPositions(it, "") }
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
        // Check if this signer fingerprint is already used in other keys
        val isSignerAlreadyUsed = checkIfSignerAlreadyUsed(signer.fingerPrint, keyName, signer.isMasterSigner)
        
        if (isSignerAlreadyUsed) {
            _uiState.update { 
                it.copy(
                    event = MiniscriptSharedWalletEvent.ShowDuplicateSignerWarning(signer, keyName)
                ) 
            }
            return
        }
        
        proceedWithAddingSigner(signer, keyName)
    }
    
    private fun proceedWithAddingSigner(signer: SignerModel, keyName: String) {
        val addressType = _uiState.value.addressType
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
    }
    
    private fun checkIfSignerAlreadyUsed(fingerPrint: String, excludeKeyName: String, isMasterSigner: Boolean): Boolean {
        val currentSigners = _uiState.value.signers
        
        // For master signers with key pattern key_x_y, check if the fingerprint is used outside the related keys group
        if (isMasterSigner && isKeyPatternXY(excludeKeyName)) {
            val prefix = getKeyPrefix(excludeKeyName)
            val relatedKeys = getAllWalletKeys()
                .filter { it.startsWith(prefix) }
            
            // Check if fingerprint is used in keys outside the related group
            val isUsedOutsideRelatedKeys = currentSigners.any { (keyName, signerModel) ->
                !relatedKeys.contains(keyName) && signerModel?.fingerPrint == fingerPrint
            }
            
            // Check taproot keys (handled through unified signers map)
            val keyPath = _uiState.value.keyPaths
            val isUsedInTaprootKey = keyPath.isNotEmpty() && 
                keyPath.any { keyName -> 
                    keyName != excludeKeyName && currentSigners[keyName]?.fingerPrint == fingerPrint 
                }
            
            return isUsedOutsideRelatedKeys || isUsedInTaprootKey
        }
        
        // For non-master signers or keys that don't follow key_x_y pattern, use original logic
        val isUsedInRegularKeys = currentSigners.any { (keyName, signerModel) ->
            keyName != excludeKeyName && signerModel?.fingerPrint == fingerPrint
        }
        
        val keyPath = _uiState.value.keyPaths
        val isUsedInTaprootKey = keyPath.isNotEmpty() && 
            keyPath.any { keyName -> 
                keyName != excludeKeyName && currentSigners[keyName]?.fingerPrint == fingerPrint 
            }
        
        return isUsedInRegularKeys || isUsedInTaprootKey
    }
    
    private fun isKeyPatternXY(keyName: String): Boolean {
        val components = keyName.split("_")
        return components.size >= 3 && components[0] == "key"
    }
    
    private fun getKeyPrefix(keyName: String): String {
        val components = keyName.split("_")
        return if (components.size >= 3) "${components[0]}_${components[1]}" else keyName
    }
    
    private fun checkIfSignerWithSamePathAlreadyUsed(fingerPrint: String, derivationPath: String, excludeKeyName: String): Boolean {
        val currentSigners = _uiState.value.signers
        val keyPath = _uiState.value.keyPaths
        
        Timber.tag(TAG).d("Checking duplicate - fingerPrint: $fingerPrint, derivationPath: $derivationPath, excludeKeyName: $excludeKeyName")
        Timber.tag(TAG).d("Current signers: ${currentSigners.mapValues { "${it.value?.fingerPrint}:${it.value?.derivationPath}" }}")
        Timber.tag(TAG).d("Key path: $keyPath")
        
        // Check if the fingerprint + derivation path combination is used in any other key
        val isUsedInSigners = currentSigners.any { (keyName, signerModel) ->
            val matches = keyName != excludeKeyName && 
                signerModel?.fingerPrint == fingerPrint && 
                signerModel.derivationPath == derivationPath
            if (matches) {
                Timber.tag(TAG).d("Found duplicate in signers - keyName: $keyName, signer: ${signerModel?.fingerPrint}:${signerModel?.derivationPath}")
            }
            matches
        }
        
        val result = isUsedInSigners
        Timber.tag(TAG).d("Duplicate check result: $result")
        
        return result
    }
    
    fun proceedWithDuplicateSigner(signer: SignerModel, keyName: String) {
        // Enable showing BIP32 paths and proceed with adding the signer
        _uiState.update { it.copy(showBip32PathForDuplicates = true) }
        proceedWithAddingSigner(signer, keyName)
    }

    private fun addSignerToState(signerModel: SignerModel, keyName: String) {
        Timber.tag(TAG).d("Adding signer: $signerModel to key: $keyName")
        
        val currentSigners = _uiState.value.signers.toMutableMap()

        // If keyName matches the pattern key_x_y, we need to handle master signer case
        val components = keyName.split("_")
        if (components.size > 2 && signerModel.isMasterSigner) {
            Timber.tag(TAG).d("Handling master signer case for key: $keyName")
            // Extract the prefix (key_x)
            val prefix = "${components[0]}_${components[1]}"

            // Find all keys in wallet that share the same prefix
            val relatedKeys = getAllWalletKeys()
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
                                    val model = singleSignerMapper(it)
                                    currentSigners[key] = model
                                    Timber.tag(TAG).d("Added signer: ${model} to key: $key")

                                    // Update UI state immediately
                                    _uiState.update { state ->
                                        state.copy(
                                            signers = currentSigners.toMap(),
                                            areAllKeysAssigned = areAllKeysAssigned(state.scriptNode, currentSigners.toMap())
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
        
        val currentSigners = _uiState.value.signers.toMutableMap()

        // Remove the signer for the specified key (works for both regular and taproot signers)
        currentSigners[keyName] = null
        Timber.tag(TAG).d("Removed signer for key: $keyName")

        _uiState.update {
            it.copy(
                signers = currentSigners,
                event = MiniscriptSharedWalletEvent.SignerRemoved(keyName),
                areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
            )
        }
        Timber.tag(TAG).d("Updated state after removing signer. Current signers: $currentSigners")
    }

    private fun areAllKeysAssigned(scriptNode: ScriptNode?, signers: Map<String, SignerModel?>): Boolean {
        if (scriptNode == null) return false
        val allKeys = getAllKeysFromScriptNode(scriptNode)
        val areScriptKeysAssigned = allKeys.all { keyName -> signers[keyName] != null }
        
        // Check if taproot keys are assigned when keyPath is present
        val keyPath = _uiState.value.keyPaths
        val scriptNodeMuSig = _uiState.value.scriptNodeMuSig
        
        val isTaprootKeyAssigned = if (keyPath.isNotEmpty()) {
            if (keyPath.size == 1) {
                // Single key scenario - check if the keyPath key has a signer assigned
                keyPath.all { keyName -> signers[keyName] != null }
            } else {
                // Multi-key scenario - check if all keys in scriptNodeMuSig are assigned
                scriptNodeMuSig?.let { muSigNode ->
                    muSigNode.keys.all { keyName -> signers[keyName] != null }
                } ?: false
            }
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

    private fun getAllWalletKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        
        // Add keys from main script node
        _uiState.value.scriptNode?.let { scriptNode ->
            keys.addAll(getAllKeysFromScriptNode(scriptNode))
        }
        
        // Add keys from MuSig node (for multi-key taproot scenarios)
        _uiState.value.scriptNodeMuSig?.let { scriptNodeMuSig ->
            keys.addAll(getAllKeysFromScriptNode(scriptNodeMuSig))
        }
        
        return keys
    }

    fun getSuggestedSigners(): List<SupportedSigner> {
        return _uiState.value.let { state ->
            state.supportedTypes.takeIf { state.addressType.isTaproot() == true }
                ?.filter { it.walletType == WalletType.MULTI_SIG || it.walletType == null }.orEmpty()
        }
    }

    fun onEventHandled() {
        Timber.tag(TAG).d("onEventHandled called, clearing event: ${_uiState.value.event}")
        _uiState.update { it.copy(event = null) }
    }

    fun changeBip32Path(keyName: String, signer: SignerModel) {
        Timber.tag(TAG).d("changeBip32Path called - keyName: $keyName, signer: ${signer.fingerPrint}")
        _uiState.update {
            it.copy(
                currentKey = keyName,
                currentSigner = signer,
                event = MiniscriptSharedWalletEvent.OpenChangeBip32Path(keyName, signer)
            )
        }
        Timber.tag(TAG).d("changeBip32Path - OpenChangeBip32Path event emitted")
        Timber.tag(TAG).d("changeBip32Path - Current uiState.event after update: ${_uiState.value.event}")
    }

    fun updateBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            val currentKey = _uiState.value.currentKey
            if (currentKey.isEmpty()) {
                return@launch
            }
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId, newPath
                )
            ).onSuccess { newSigner ->
                Timber.tag(TAG).d("updateBip32Path - Got new signer for path $newPath: $newSigner")
                // After getting the new signer, check if passphrase is needed
                getMasterSignerUseCase(
                    masterSignerId
                ).onSuccess { masterSigner ->
                    Timber.tag(TAG).d("updateBip32Path - Got master signer: $masterSigner")
                    if (masterSigner.device.needPassPhraseSent == true) {
                        Timber.tag(TAG).d("Master signer requires passphrase, emitting RequestPassphrase event")
                        _uiState.update {
                            it.copy(
                                pendingPassphraseState = PendingPassphraseState(
                                    masterSignerId = masterSignerId,
                                    newPath = newPath,
                                    currentKey = currentKey,
                                    newSigner = newSigner
                                ),
                                event = MiniscriptSharedWalletEvent.RequestPassphrase
                            )
                        }
                        return@launch
                    } else {
                        Timber.tag(TAG).d("Master signer does not require passphrase, continuing with normal flow")
                        // No passphrase needed, continue with the normal flow
                        continueWithBip32Update(newSigner, currentKey, masterSignerId, newPath)
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).e("Failed to get master signer: $error")
                    _uiState.update {
                        it.copy(
                            event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                            currentKey = "",
                            pendingBip32Update = null
                        )
                    }
                }
            }.onFailure { error ->
                if (error is NCNativeException && error.message.contains("-1009")) {
                    savedStateHandle[NEW_PATH] = newPath
                    _uiState.update { 
                        it.copy(
                            requestCacheTapSignerXpubEvent = true,
                            pendingBip32Update = PendingBip32Update(masterSignerId, newPath, currentKey)
                        ) 
                    }
                } else {
                    Timber.tag(TAG).e("Failed to change bip32 path $error")
                    _uiState.update {
                        it.copy(
                            event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                            currentKey = "",
                            pendingBip32Update = null
                        )
                    }
                }
            }
        }
    }
    
    private suspend fun continueWithBip32Update(newSigner: SingleSigner, currentKey: String, masterSignerId: String, newPath: String) {
        val newSignerModel = singleSignerMapper(newSigner)

        // Check if this update would create a duplicate signer (same fingerprint + same derivation path)
        val wouldCreateDuplicate = checkIfSignerWithSamePathAlreadyUsed(
            newSignerModel.fingerPrint, 
            newSignerModel.derivationPath, 
            currentKey
        )

        if (wouldCreateDuplicate) {
            Timber.tag(TAG).d("BIP32 Update - Showing duplicate warning dialog")
            // Store the pending update and show warning dialog
            _uiState.update {
                it.copy(
                    event = MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning(
                        newSignerModel, 
                        currentKey
                    ),
                    pendingBip32Update = PendingBip32Update(masterSignerId, newPath, currentKey)
                )
            }
        } else {
            Timber.tag(TAG).d("BIP32 Update - No duplicate found, proceeding with update")
            // Proceed with the update if no duplicate
            proceedWithBip32Update(newSignerModel, currentKey)
        }
        
//        clearSignerPassphraseUseCase(newSignerModel.fingerPrint)
    }
    
    private fun proceedWithBip32Update(newSignerModel: SignerModel, currentKey: String) {
        // Update the signer in the unified signers map (works for both regular and taproot signers)
        val currentSigners = _uiState.value.signers.toMutableMap()
        currentSigners[currentKey] = newSignerModel
        _uiState.update {
            it.copy(
                signers = currentSigners,
                event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSignerModel),
                currentKey = "",
                pendingBip32Update = null,
                areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
            )
        }
    }
    
    fun proceedWithDuplicateBip32Update() {
        val pendingUpdate = _uiState.value.pendingBip32Update ?: return
        
        viewModelScope.launch {
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    pendingUpdate.masterSignerId, 
                    pendingUpdate.newPath
                )
            ).onSuccess { newSigner ->
                val newSignerModel = singleSignerMapper(newSigner)
                
                // Enable showing BIP32 paths and proceed with the update
                _uiState.update { it.copy(showBip32PathForDuplicates = true) }
                proceedWithBip32Update(newSignerModel, pendingUpdate.currentKey)
                
            }.onFailure { error ->
                Timber.e("Failed to proceed with duplicate bip32 update: $error")
                _uiState.update {
                    it.copy(
//                        event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                        pendingBip32Update = null
                    )
                }
            }
        }
    }

    fun submitPassphrase(passphrase: String) {
        Timber.tag(TAG).d("submitPassphrase called with passphrase length: ${passphrase.length}")
        val pendingState = _uiState.value.pendingPassphraseState ?: run {
            Timber.tag(TAG).e("submitPassphrase called but no pending passphrase state found")
            return
        }
        
        viewModelScope.launch {

            sendSignerPassphraseUseCase(
                SendSignerPassphraseUseCase.Param(
                    signerId = pendingState.masterSignerId,
                    passphrase = passphrase
                )
            ).onSuccess {
                // Passphrase sent successfully, continue with the BIP32 update flow
                val newSignerModel = singleSignerMapper(pendingState.newSigner)
                val currentKey = pendingState.currentKey
                
                Timber.tag(TAG).d("Passphrase sent successfully, continuing with BIP32 update")
                
                // Check if this update would create a duplicate signer
                val wouldCreateDuplicate = checkIfSignerWithSamePathAlreadyUsed(
                    newSignerModel.fingerPrint, 
                    newSignerModel.derivationPath, 
                    currentKey
                )
                
                if (wouldCreateDuplicate) {
                    // Show duplicate warning dialog
                    _uiState.update {
                        it.copy(
                            event = MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning(
                                newSignerModel, 
                                currentKey
                            ),
                            pendingBip32Update = PendingBip32Update(
                                pendingState.masterSignerId, 
                                pendingState.newPath, 
                                currentKey
                            ),
                            pendingPassphraseState = null
                        )
                    }
                    return@launch
                }
                
                // No duplicate found, proceed with the update
                proceedWithBip32Update(newSignerModel, currentKey)
                _uiState.update { it.copy(pendingPassphraseState = null) }
                
            }.onFailure { error ->
                Timber.e("Failed to send passphrase: $error")
                _uiState.update {
                    it.copy(
//                        event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                        pendingPassphraseState = null
                    )
                }
            }
        }
    }

    fun getKeySignerMap(): Map<String, SingleSigner> {
        val result = mutableMapOf<String, SingleSigner>()
        val scriptNode = _uiState.value.scriptNode ?: return result
        val scriptNodeMuSig = _uiState.value.scriptNodeMuSig
        val signers = _uiState.value.signers
        val keyPath = _uiState.value.keyPaths

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
        
        // Add keys from scriptNodeMuSig if available (for MuSig scenarios)
        scriptNodeMuSig?.let { musigNode ->
            Timber.tag(TAG).d("Adding MuSig keys to keySignerMap: ${musigNode.keys}")
            musigNode.keys.forEach { key ->
                signers[key]?.let { signerModel ->
                    result[key] = signerModel.toSingleSigner()
                }
            }
        }
        
        // Add taproot keys from keyPath (handled uniformly through signers map)
        keyPath.forEach { key ->
            signers[key]?.let { signerModel ->
                Timber.tag(TAG).d("Adding taproot key to keySignerMap: $key -> $signerModel")
                result[key] = signerModel.toSingleSigner()
            }
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
                    val newSignerModel = singleSignerMapper(newSigner)
                    
                    // Check if this update would create a duplicate (only if it's a BIP32 path change)
                    val pendingUpdate = _uiState.value.pendingBip32Update
                    if (pendingUpdate != null) {
                        Timber.tag(TAG).d("TapSigner Cache - Pending BIP32 update found, checking for duplicates")
                        val wouldCreateDuplicate = checkIfSignerWithSamePathAlreadyUsed(
                            newSignerModel.fingerPrint, 
                            newSignerModel.derivationPath, 
                            currentKey
                        )
                        
                        if (wouldCreateDuplicate) {
                            Timber.tag(TAG).d("TapSigner Cache - Duplicate found, showing warning dialog")
                            // Store the pending update and show warning dialog
                            _uiState.update {
                                it.copy(
                                    event = MiniscriptSharedWalletEvent.ShowDuplicateSignerUpdateWarning(
                                        newSignerModel, 
                                        currentKey
                                    ),
                                    requestCacheTapSignerXpubEvent = false
                                )
                            }
                            return@launch
                        }
                        Timber.tag(TAG).d("TapSigner Cache - No duplicate found")
                    } else {
                        Timber.tag(TAG).d("TapSigner Cache - No pending BIP32 update found")
                    }
                    
                    // Proceed with the update
                    proceedWithBip32Update(newSignerModel, currentKey)
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
                            requestCacheTapSignerXpubEvent = false,
                            pendingBip32Update = null
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
                            val model = singleSignerMapper(it)
                            currentSigners[key] = model
                            Timber.tag(TAG).d("Resumed - Added signer: ${model} to key: $key")

                            // Update UI state immediately
                            _uiState.update { state ->
                                state.copy(
                                    signers = currentSigners.toMap(),
                                    areAllKeysAssigned = areAllKeysAssigned(state.scriptNode, currentSigners.toMap())
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

    fun clearAndResetState() {
        // Clear all state to create a "fresh instance" effect
        // This will be called when navigating to configure screen to ensure fresh state
        singleSigners.clear()
        masterSigners.clear()
        keyPositionMap.clear()
        currentKeyToAssign = ""
        oldSigners = emptySet()
        pendingAddSignerState = null
        
        // Preserve important values that should not be reset
        val currentState = _uiState.value
        _uiState.value = MiniscriptSharedWalletState(
            isTestNet = currentState.isTestNet,
            reuseSigner = currentState.reuseSigner,
        )
        
        // Reload basic info
        loadInfo()
    }

    companion object {
        private const val NEW_PATH = "new_path"
        private const val TAG = "miniscript-feature"
    }
}

data class MiniscriptSharedWalletState(
    val scriptNode: ScriptNode? = null,
    val scriptNodeMuSig: ScriptNode? = null,
    val keyPaths: List<String> = emptyList(),
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
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val isTestNet: Boolean = false,
    val currentKeyToAssign: String = "",
    val showBip32PathForDuplicates: Boolean = false,
    val pendingBip32Update: PendingBip32Update? = null,
    val pendingPassphraseState: PendingPassphraseState? = null,
    val reuseSigner: Boolean = false
)

sealed class MiniscriptSharedWalletEvent {
    data class Error(val message: String) : MiniscriptSharedWalletEvent()
    data class SignerAdded(val keyName: String, val signer: SignerModel) :
        MiniscriptSharedWalletEvent()

    data class SignerRemoved(val keyName: String) : MiniscriptSharedWalletEvent()
    data class OpenChangeBip32Path(val keyName: String, val signer: SignerModel) :
        MiniscriptSharedWalletEvent()

    data class Bip32PathChanged(val signer: SignerModel) : MiniscriptSharedWalletEvent()
    data class CreateWalletSuccess(val wallet: Wallet) : MiniscriptSharedWalletEvent()
    data object RequestCacheTapSignerXpub : MiniscriptSharedWalletEvent()
    data class ShowDuplicateSignerWarning(val signer: SignerModel, val keyName: String) : MiniscriptSharedWalletEvent()
    data class ShowDuplicateSignerUpdateWarning(val signer: SignerModel, val keyName: String) : MiniscriptSharedWalletEvent()
    data object RequestPassphrase : MiniscriptSharedWalletEvent()
}

data class PendingAddSignerState(
    val signerModel: SignerModel,
    val keyName: String,
    val relatedKeys: List<String>,
    val currentIndex: Int,
    val processedKeyIndex: Int
)

data class PendingBip32Update(
    val masterSignerId: String,
    val newPath: String,
    val currentKey: String
)

data class PendingPassphraseState(
    val masterSignerId: String,
    val newPath: String,
    val currentKey: String,
    val newSigner: SingleSigner
)