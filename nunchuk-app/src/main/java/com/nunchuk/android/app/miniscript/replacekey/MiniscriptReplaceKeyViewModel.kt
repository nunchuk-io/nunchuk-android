package com.nunchuk.android.app.miniscript.replacekey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.compose.miniscript.ReplaceKeyData
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TimeLock
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateMiniscriptWalletUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.miniscript.GetTimelockTemplateUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MiniscriptReplaceKeyState(
    val walletExtended: WalletExtended = WalletExtended(wallet = Wallet()),
    val scriptNode: ScriptNode? = null,
    val signerMap: Map<String, SignerModel?> = emptyMap(),
    val scriptNodeMuSig: ScriptNode? = null,
    val muSigSignerMap: Map<String, SignerModel?> = emptyMap(),
    val isLoading: Boolean = false,
    val replaceKeyData: Map<String, ReplaceKeyData> = emptyMap(),
    val allSigners: List<SignerModel> = emptyList(),
    val supportedTypes: List<SupportedSigner> = emptyList(),
    val currentKeyToReplace: String = ""
)

sealed class MiniscriptReplaceKeyEvent {
    data class Error(val message: String) : MiniscriptReplaceKeyEvent()
    data class CreateWalletSuccess(val wallet: Wallet) : MiniscriptReplaceKeyEvent()
}

@HiltViewModel
class MiniscriptReplaceKeyViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val masterSignerMapper: MasterSignerMapper,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val createMiniscriptWalletUseCase: CreateMiniscriptWalletUseCase,
    private val getTimelockTemplateUseCase: GetTimelockTemplateUseCase,
    private val accountManager: AccountManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(MiniscriptReplaceKeyState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<MiniscriptReplaceKeyEvent>()
    val event = _event.asSharedFlow()

    private val walletId: String
        get() = savedStateHandle.get<String>("walletId").orEmpty()

    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()

    init {
        getWalletDetails()
        loadSigners()
        loadSupportedSigners()
    }
    
    private fun loadSigners() {
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
                    .map { signer -> masterSignerMapper(signer) } + 
                    singleSigner.map { signer -> signer.toModel() }
                _state.update { it.copy(allSigners = signers) }
            }
        }
    }
    
    private fun loadSupportedSigners() {
        viewModelScope.launch {
            getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
                _state.update { it.copy(supportedTypes = supportedTypes) }
            }
        }
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getWalletUseCase.execute(walletId)
                .map { walletExtended ->
                    MiniscriptReplaceKeyState(
                        walletExtended = walletExtended,
                        isLoading = false
                    )
                }
                .flowOn(Dispatchers.IO)
                .onException {
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(MiniscriptReplaceKeyEvent.Error(it.message.orUnknownError()))
                }
                .flowOn(Dispatchers.Main)
                .collect { newState ->
                    _state.update {
                        it.copy(
                            walletExtended = newState.walletExtended,
                            isLoading = newState.isLoading
                        )
                    }
                    getMiniscriptInfo()
                }
        }
    }

    private suspend fun getMiniscriptInfo() {
        val wallet = _state.value.walletExtended.wallet
        if (wallet.miniscript.isNotEmpty()) {
            getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
                _state.update { it.copy(scriptNode = result.scriptNode) }
                val signerMap = parseSignersFromScriptNode(result.scriptNode)
                _state.update { it.copy(signerMap = signerMap) }

                if (wallet.addressType == AddressType.TAPROOT && wallet.walletTemplate != WalletTemplate.DISABLE_KEY_PATH) {
                    if (wallet.totalRequireSigns > 1) {
                        val scriptNodeMuSig = MiniscriptUtil.buildMusigNode(wallet.totalRequireSigns)
                        val muSigSignerMap = scriptNodeMuSig.keys.mapIndexed { index, key ->
                            key to wallet.signers.getOrNull(index)?.toModel()
                        }.toMap()

                        _state.update {
                            it.copy(
                                scriptNodeMuSig = scriptNodeMuSig,
                                muSigSignerMap = muSigSignerMap,
                            )
                        }
                    }
                }
            }.onFailure {
                _event.emit(MiniscriptReplaceKeyEvent.Error(it.message.orUnknownError()))
            }
        } else {
            _state.update { it.copy(scriptNode = null) }
        }
    }

    private suspend fun parseSignersFromScriptNode(node: ScriptNode): Map<String, SignerModel?> {
        val signerMap = mutableMapOf<String, SignerModel?>()
        node.keys.forEach { key ->
            val signer = parseSignerStringUseCase(key).getOrNull()
            signer?.let {
                signerMap[key] = singleSignerMapper(signer)
            }
        }
        node.subs.forEach { subNode ->
            signerMap.putAll(parseSignersFromScriptNode(subNode))
        }
        return signerMap
    }

    fun updateTimelockReplaceData(originalNode: ScriptNode, timelockData: com.nunchuk.android.app.miniscript.contractpolicy.TimelockData) {
        val nodeId = originalNode.idString
        
        viewModelScope.launch {
            // Get the new type based on timelockData.timelockType
            val newType = when (timelockData.timelockType) {
                com.nunchuk.android.type.MiniscriptTimelockType.ABSOLUTE -> ScriptNodeType.AFTER.name
                com.nunchuk.android.type.MiniscriptTimelockType.RELATIVE -> ScriptNodeType.OLDER.name
            }
            
            // Create a new TimeLock with the updated value
            val newTimeLock = TimeLock(
                value = timelockData.value,
                based = timelockData.timeUnit,
                type = timelockData.timelockType
            )
            
            // Create a new ScriptNode with the updated timelock and type
            val replacedNode = ScriptNode(
                id = originalNode.id,
                type = newType,
                keys = originalNode.keys,
                subs = originalNode.subs,
                k = originalNode.k,
                data = originalNode.data,
                timeLock = newTimeLock
            )
            
            _state.update { currentState ->
                val updatedReplaceKeyData = currentState.replaceKeyData.toMutableMap()
                updatedReplaceKeyData[nodeId] = ReplaceKeyData(
                    originalTimeLock = originalNode,
                    replacedTimeLock = replacedNode
                )
                currentState.copy(replaceKeyData = updatedReplaceKeyData)
            }
            
            Timber.tag(TAG).d("Timelock replaced for node $nodeId: original=${originalNode.timeLock}, new=$newTimeLock")
        }
    }
    
    fun removeReplacedTimelock(nodeId: String) {
        Timber.tag(TAG).d("Removing replaced timelock for node: $nodeId")
        
        _state.update { currentState ->
            val updatedReplaceKeyData = currentState.replaceKeyData.toMutableMap()
            updatedReplaceKeyData.remove(nodeId)
            currentState.copy(replaceKeyData = updatedReplaceKeyData)
        }
    }
    
    fun setCurrentKeyToReplace(keyName: String) {
        _state.update { it.copy(currentKeyToReplace = keyName) }
    }
    
    fun replaceSignerWithExisting(newSigner: SignerModel, keyName: String) {
        Timber.tag(TAG).d("Replacing signer for key: $keyName with: ${newSigner.name}")
        
        val addressType = _state.value.walletExtended.wallet.addressType
        
        viewModelScope.launch {
            val signerToAdd = if (newSigner.isMasterSigner) {
                val masterSigner = masterSigners.find { it.id == newSigner.fingerPrint } ?: return@launch
                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        addressType
                    )
                ).getOrNull()?.toModel()
            } else {
                val singleSigner = singleSigners.find {
                    it.masterFingerprint == newSigner.fingerPrint &&
                            it.derivationPath == newSigner.derivationPath
                } ?: return@launch
                singleSigner.toModel()
            }
            
            signerToAdd?.let { replacementSigner ->
                val currentSignerMap = _state.value.signerMap.toMutableMap()
                val currentMuSigSignerMap = _state.value.muSigSignerMap.toMutableMap()
                val currentReplaceKeyData = _state.value.replaceKeyData.toMutableMap()
                
                // Get the original signer before replacing
                val originalSigner = currentSignerMap[keyName] ?: currentMuSigSignerMap[keyName]
                
                // Update the signer map
                if (currentSignerMap.containsKey(keyName)) {
                    currentSignerMap[keyName] = replacementSigner
                } else if (currentMuSigSignerMap.containsKey(keyName)) {
                    currentMuSigSignerMap[keyName] = replacementSigner
                }
                
                // Store the replacement data
                val existingData = currentReplaceKeyData[keyName]
                currentReplaceKeyData[keyName] = ReplaceKeyData(
                    originalSigner = originalSigner,
                    replacedSigner = replacementSigner,
                    originalTimeLock = existingData?.originalTimeLock,
                    replacedTimeLock = existingData?.replacedTimeLock
                )
                
                _state.update {
                    it.copy(
                        signerMap = currentSignerMap,
                        muSigSignerMap = currentMuSigSignerMap,
                        replaceKeyData = currentReplaceKeyData,
                        currentKeyToReplace = ""
                    )
                }
                
                Timber.tag(TAG).d("Signer replaced: original=${originalSigner?.name}, new=${replacementSigner.name}")
            }
        }
    }
    
    fun removeReplacedSigner(keyName: String) {
        Timber.tag(TAG).d("Removing replaced signer for key: $keyName")
        
        val currentReplaceKeyData = _state.value.replaceKeyData.toMutableMap()
        val replaceData = currentReplaceKeyData[keyName] ?: return
        val originalSigner = replaceData.originalSigner ?: return
        
        val currentSignerMap = _state.value.signerMap.toMutableMap()
        val currentMuSigSignerMap = _state.value.muSigSignerMap.toMutableMap()
        
        // Restore the original signer
        if (currentSignerMap.containsKey(keyName)) {
            currentSignerMap[keyName] = originalSigner
        } else if (currentMuSigSignerMap.containsKey(keyName)) {
            currentMuSigSignerMap[keyName] = originalSigner
        }
        
        // Clear the replacement data for this key
        currentReplaceKeyData[keyName] = ReplaceKeyData(
            originalSigner = null,
            replacedSigner = null,
            originalTimeLock = replaceData.originalTimeLock,
            replacedTimeLock = replaceData.replacedTimeLock
        )
        
        _state.update {
            it.copy(
                signerMap = currentSignerMap,
                muSigSignerMap = currentMuSigSignerMap,
                replaceKeyData = currentReplaceKeyData
            )
        }
        
        Timber.tag(TAG).d("Restored original signer: ${originalSigner.name}")
    }
    
    fun createMiniscriptWallet() {
        val currentState = _state.value
        val wallet = currentState.walletExtended.wallet
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Build the updated miniscript template with replaced timelocks
                val updatedTemplate = buildUpdatedMiniscriptTemplate(
                    originalTemplate = wallet.miniscript,
                    scriptNode = currentState.scriptNode,
                    replaceKeyData = currentState.replaceKeyData
                )
                
                Timber.tag(TAG).d("Updated miniscript template: $updatedTemplate")
                
                // Build the signer map with replaced signers
                val signerMap = getKeySignerMap()
                
                if (signerMap.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(MiniscriptReplaceKeyEvent.Error("No signers assigned."))
                    return@launch
                }
                
                createMiniscriptWalletUseCase(
                    CreateMiniscriptWalletUseCase.Params(
                        miniscriptTemplate = updatedTemplate,
                        signerMap = signerMap,
                        name = wallet.name,
                        description = wallet.description,
                        addressType = wallet.addressType.ordinal,
                        allowUsedSigner = true,
                        decoyPin = accountManager.getAccount().decoyPin
                    )
                ).onSuccess { createdWallet ->
                    Timber.tag(TAG).d("Wallet created successfully: ${createdWallet.id}")
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(MiniscriptReplaceKeyEvent.CreateWalletSuccess(createdWallet))
                }.onFailure { error ->
                    Timber.tag(TAG).e("Failed to create miniscript wallet: $error")
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(MiniscriptReplaceKeyEvent.Error(error.message.orUnknownError()))
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Exception in createMiniscriptWallet: $e")
                _state.update { it.copy(isLoading = false) }
                _event.emit(MiniscriptReplaceKeyEvent.Error(e.message.orUnknownError()))
            }
        }
    }
    
    private suspend fun buildUpdatedMiniscriptTemplate(
        originalTemplate: String,
        scriptNode: ScriptNode?,
        replaceKeyData: Map<String, ReplaceKeyData>
    ): String {
        var updatedTemplate = originalTemplate
        
        // Find all timelock nodes and replace their values if there's a replacement
        scriptNode?.let { node ->
            val timelockReplacements = collectTimelockReplacements(node, replaceKeyData)
            
            for ((originalTimeLock, replacedTimeLock) in timelockReplacements) {
                // Get the template string for the original timelock
                val originalTemplateStr = getTimelockTemplateUseCase(originalTimeLock).getOrNull()
                // Get the template string for the replaced timelock
                val replacedTemplateStr = getTimelockTemplateUseCase(replacedTimeLock).getOrNull()
                
                if (originalTemplateStr != null && replacedTemplateStr != null) {
                    Timber.tag(TAG).d("Replacing timelock: $originalTemplateStr -> $replacedTemplateStr")
                    updatedTemplate = updatedTemplate.replace(originalTemplateStr, replacedTemplateStr)
                }
            }
        }
        
        return updatedTemplate
    }
    
    private fun collectTimelockReplacements(
        node: ScriptNode,
        replaceKeyData: Map<String, ReplaceKeyData>
    ): List<Pair<TimeLock, TimeLock>> {
        val replacements = mutableListOf<Pair<TimeLock, TimeLock>>()
        
        // Check if this node is a timelock node with replacement data
        if (node.type == ScriptNodeType.AFTER.name || node.type == ScriptNodeType.OLDER.name) {
            val nodeId = node.idString
            val data = replaceKeyData[nodeId]
            val originalTimeLock = data?.originalTimeLock?.timeLock
            val replacedTimeLock = data?.replacedTimeLock?.timeLock
            
            if (originalTimeLock != null && replacedTimeLock != null) {
                replacements.add(originalTimeLock to replacedTimeLock)
            }
        }
        
        // Recursively check sub-nodes
        node.subs.forEach { subNode ->
            replacements.addAll(collectTimelockReplacements(subNode, replaceKeyData))
        }
        
        return replacements
    }
    
    private fun getKeySignerMap(): Map<String, SingleSigner> {
        val result = mutableMapOf<String, SingleSigner>()
        val currentState = _state.value
        val scriptNode = currentState.scriptNode ?: return result
        val scriptNodeMuSig = currentState.scriptNodeMuSig
        val signers = currentState.signerMap
        val muSigSigners = currentState.muSigSignerMap
        val replaceKeyData = currentState.replaceKeyData
        
        fun traverseNode(node: ScriptNode) {
            // Process keys in current node
            node.keys.forEach { key ->
                // Use replaced signer if available, otherwise use current signer
                val signer = replaceKeyData[key]?.replacedSigner ?: signers[key]
                signer?.let { signerModel ->
                    result[key] = signerModel.toSingleSigner()
                }
            }
            
            // Recursively process sub-nodes
            node.subs.forEach { subNode ->
                traverseNode(subNode)
            }
        }
        
        traverseNode(scriptNode)
        
        // Process MuSig keys
        scriptNodeMuSig?.let { muSigNode ->
            muSigNode.keys.forEach { key ->
                val signer = replaceKeyData[key]?.replacedSigner ?: muSigSigners[key]
                signer?.let { signerModel ->
                    result[key] = signerModel.toSingleSigner()
                }
            }
        }
        
        return result
    }
    
    companion object {
        private const val TAG = "miniscript-feature"
    }
}
