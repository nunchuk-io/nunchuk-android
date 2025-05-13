package com.nunchuk.android.app.miniscript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.miniscript.configurewallet.MiniscriptConfigureWallet
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
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
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiniscriptSharedWalletState())
    val uiState = _uiState.asStateFlow()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()
    private val keyPositionMap = mutableMapOf<String, String>()
    private var currentKeyToAssign: String = ""
    private var oldSigners: Set<String> = emptySet()

    init {
        loadInfo()
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                .collect {
                    Timber.d("Pushing event: $it")
                    if (currentKeyToAssign.isNotEmpty()) {
                        addSignerToState(it.signer.toModel(), currentKeyToAssign)
                        currentKeyToAssign = ""
                        loadInfo()
                    }
                }
        }
        viewModelScope.launch {
            getChainTipUseCase(Unit).onSuccess { blockHeight ->
                Timber.tag("miniscript-feature").d("MiniscriptSharedWalletViewModel - blockHeight: $blockHeight")
                _uiState.update { it.copy(currentBlockHeight = blockHeight) }
            }.onFailure { error ->
                Timber.tag("miniscript-feature").e("MiniscriptSharedWalletViewModel - error: $error")
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

                Timber.tag("miniscript-feature").d("Loaded signers: $signers")
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
                _uiState.update {
                    it.copy(
                        scriptNode = result,
                        areAllKeysAssigned = areAllKeysAssigned(result, it.signers)
                    )
                }
                mapKeyPositions(result, "")
            }.onFailure {
                Timber.tag("miniscript-feature").e("MiniscriptSharedWalletViewModel - error: $it")
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
        Timber.tag("miniscript-feature").d("Adding signer: $signerModel to key: $keyName")
        val currentSigners = _uiState.value.signers.toMutableMap()
        
        // If keyName matches the pattern key_x_y, we need to handle master signer case
        val components = keyName.split("_")
        if (components.size > 2 && signerModel.isMasterSigner) {
            Timber.tag("miniscript-feature").d("Handling master signer case for key: $keyName")
            // Extract the prefix (key_x)
            val prefix = "${components[0]}_${components[1]}"
            
            // Find all keys in scriptNode that share the same prefix
            val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                .filter { it.startsWith(prefix) }
                .sorted() // Sort to ensure consistent order
            
            Timber.tag("miniscript-feature").d("Found related keys: $relatedKeys")
            
            // Get the first available index from the master signer
            viewModelScope.launch {
                getCurrentIndexFromMasterSignerUseCase(
                    GetCurrentIndexFromMasterSignerUseCase.Param(
                        xfp = signerModel.fingerPrint,
                        walletType = WalletType.MULTI_SIG,
                        addressType = _uiState.value.addressType
                    )
                ).onSuccess { startIndex ->
                    Timber.tag("miniscript-feature").d("Got start index: $startIndex for master signer: ${signerModel.fingerPrint}")
                    // For each related key, get a signer with increasing index
                    var currentIndex = startIndex
                    relatedKeys.forEach { key ->
                        // Only assign a signer if the key hasn't been assigned yet
                        if (currentSigners[key] == null) {
                            Timber.tag("miniscript-feature").d("Getting signer at index: $currentIndex for unassigned key: $key")
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
                                    Timber.tag("miniscript-feature").d("Added signer: ${it.toModel()} to key: $key")
                                } ?: run {
                                    Timber.tag("miniscript-feature").e("No signer found for key: $key at index: $currentIndex")
                                }
                            }.onFailure {
                                Timber.tag("miniscript-feature").e("Failed to get signer for key: $key at index: $currentIndex, error: $it")
                            }
                            currentIndex++
                        } else {
                            Timber.tag("miniscript-feature").d("Skipping already assigned key: $key with signer: ${currentSigners[key]}")
                        }
                    }
                    
                    _uiState.update {
                        it.copy(
                            signers = currentSigners,
                            event = MiniscriptSharedWalletEvent.SignerAdded(keyName, signerModel),
                            areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
                        )
                    }
                    Timber.tag("miniscript-feature").d("Updated state with new signers: $currentSigners")
                }
            }
        } else {
            Timber.tag("miniscript-feature").d("Handling non-master signer case or old key format")
            // Handle non-master signer case or old key format
            currentSigners[keyName] = signerModel
            
            _uiState.update {
                it.copy(
                    signers = currentSigners,
                    event = MiniscriptSharedWalletEvent.SignerAdded(keyName, signerModel),
                    areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
                )
            }
            Timber.tag("miniscript-feature").d("Updated state with new signer: $signerModel for key: $keyName")
        }
    }

    fun removeSigner(keyName: String) {
        Timber.tag("miniscript-feature").d("Removing signer for key: $keyName")
        val currentSigners = _uiState.value.signers.toMutableMap()
        
        // If keyName matches the pattern key_x_y, we need to handle related keys
        val components = keyName.split("_")
        if (components.size > 2) {
            Timber.tag("miniscript-feature").d("Handling key with pattern key_x_y")
            // Extract the prefix (key_x)
            val prefix = "${components[0]}_${components[1]}"
            
            // Get the signer to remove
            val signerToRemove = currentSigners[keyName]
            Timber.tag("miniscript-feature").d("Signer to remove: $signerToRemove")
            
            // If it's a master signer, remove all related keys with the same master fingerprint
            if (signerToRemove?.isMasterSigner == true) {
                val masterFingerprintToRemove = signerToRemove.fingerPrint
                Timber.tag("miniscript-feature").d("Removing all related keys with master fingerprint: $masterFingerprintToRemove")
                
                // Find all keys in scriptNode that share the same prefix
                val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                    .filter { it.startsWith(prefix) }
                
                Timber.tag("miniscript-feature").d("Found related keys to remove: $relatedKeys")
                
                // Remove signers for all related keys with the same master fingerprint
                relatedKeys.forEach { key ->
                    if (currentSigners[key]?.fingerPrint == masterFingerprintToRemove) {
                        currentSigners[key] = null
                        Timber.tag("miniscript-feature").d("Removed signer for key: $key")
                    }
                }
            } else {
                // For non-master signers, just remove the specific key
                currentSigners[keyName] = null
                Timber.tag("miniscript-feature").d("Removed non-master signer for key: $keyName")
            }
        } else {
            // Handle old key format
            currentSigners[keyName] = null
            Timber.tag("miniscript-feature").d("Removed signer for old format key: $keyName")
        }
        
        _uiState.update {
            it.copy(
                signers = currentSigners,
                event = MiniscriptSharedWalletEvent.SignerRemoved(keyName),
                areAllKeysAssigned = areAllKeysAssigned(it.scriptNode, currentSigners)
            )
        }
        Timber.tag("miniscript-feature").d("Updated state after removing signer(s). Current signers: $currentSigners")
    }

    private fun areAllKeysAssigned(scriptNode: ScriptNode?, signers: Map<String, SignerModel?>): Boolean {
        if (scriptNode == null) return false
        val allKeys = getAllKeysFromScriptNode(scriptNode)
        return allKeys.all { keyName -> signers[keyName] != null }
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
                val currentSigners = _uiState.value.signers.toMutableMap()
                // Only update the current key's signer
                currentSigners[currentKey] = newSigner.toModel()
                _uiState.update {
                    it.copy(
                        signers = currentSigners,
                        event = MiniscriptSharedWalletEvent.Bip32PathChanged(newSigner.toModel()),
                        currentKey = ""
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError()),
                        currentKey = ""
                    )
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
                Timber.tag("miniscript-feature").d("About to emit CreateWalletSuccess event with wallet: $wallet")
                _uiState.update {
                    it.copy(
                        event = MiniscriptSharedWalletEvent.CreateWalletSuccess(wallet)
                    )
                }
                Timber.tag("miniscript-feature").d("CreateWalletSuccess event emitted")
            }.onFailure { error ->
                Timber.e("Failed to create miniscript wallet: $error")
                _uiState.update { it.copy(event = MiniscriptSharedWalletEvent.Error(error.message.orUnknownError())) }
            }
        }
    }
}

data class MiniscriptSharedWalletState(
    val scriptNode: ScriptNode? = null,
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
    val currentBlockHeight: Int = 0
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
} 