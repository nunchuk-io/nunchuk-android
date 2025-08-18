package com.nunchuk.android.main.groupwallet

import android.app.Application
import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetGroupDeviceUIDUseCase
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.listener.GroupDeleteListener
import com.nunchuk.android.listener.GroupOnlineListener
import com.nunchuk.android.listener.GroupSandboxListener
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.CreateGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.CreateReplaceGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.DeleteGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.FinalizeGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupOnlineUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.RemoveSignerFromGroupUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetCurrentIndexFromMasterSignerUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerByIndexUseCase
import com.nunchuk.android.usecase.signer.GetSignerUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import com.nunchuk.android.usecase.signer.SetSlotOccupiedUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

const val KEY_NOT_SYNCED_NAME = "ADDED"

@HiltViewModel
class FreeGroupWalletViewModel @Inject constructor(
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val createGroupSandboxUseCase: CreateGroupSandboxUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val singleSignerMapper: SingleSignerMapper,
    private val savedStateHandle: SavedStateHandle,
    private val addSignerToGroupUseCase: AddSignerToGroupUseCase,
    private val removeSignerFromGroupUseCase: RemoveSignerFromGroupUseCase,
    private val deleteGroupSandboxUseCase: DeleteGroupSandboxUseCase,
    private val getSignerUseCase: GetSignerUseCase,
    private val getGroupOnlineUseCase: GetGroupOnlineUseCase,
    private val pushEventManager: PushEventManager,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val getCurrentIndexFromMasterSignerUseCase: GetCurrentIndexFromMasterSignerUseCase,
    private val getSignerFromMasterSignerByIndexUseCase: GetSignerFromMasterSignerByIndexUseCase,
    private val setSlotOccupiedUseCase: SetSlotOccupiedUseCase,
    private val getGroupDeviceUIDUseCase: GetGroupDeviceUIDUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val createReplaceGroupUseCase: CreateReplaceGroupUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val finalizeGroupSandboxUseCase: FinalizeGroupSandboxUseCase,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val application: Application,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
) : ViewModel() {
    val groupId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_GROUP_ID).orEmpty()
    private val replaceWalletId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_REPLACE_WALLET_ID)
            .orEmpty()
    private val isRecoverWallet: Boolean
        get() = savedStateHandle.get<Boolean>(FreeGroupWalletActivity.EXTRA_IS_RECOVER_WALLET) == true

    private val _uiState = MutableStateFlow(FreeGroupWalletUiState())
    val uiState: StateFlow<FreeGroupWalletUiState> = _uiState.asStateFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()
    private var deviceUID: String = ""
    private var addSignerJob: Job? = null
    private var wallet: Wallet? = null
    private var groupUpdateJob: Job? = null
    
    // State variables for handling TapSigner caching during addSignerToGroupWithKeyName
    private var pendingAddSignerState: PendingAddSignerState? = null

    init {
        loadSigners()
        listenGroupSandbox()
        listenGroupOnline()
        listenGroupDelete()
        if (replaceWalletId.isEmpty() && isRecoverWallet.not()) {
            if (groupId.isEmpty()) {
                createGroupSandbox()
            } else {
                getGroupOnline()
            }
        }
        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                .collect {
                    Timber.d("Pushing event: $it")
                    if (isMiniscriptWallet() && _uiState.value.currentKeyToAssign.isNotEmpty()) {
                        // For Miniscript wallets, add the signer to the specific key
                        val keyName = _uiState.value.currentKeyToAssign
                        Timber.tag("miniscript-feature").d("PushEvent: Adding signer to Miniscript key: $keyName")
                        val singleSigner = it.signer
                        
                        // Check if this is key_x_y pattern with master signer
                        if (isKeyPatternXY(keyName) && singleSigner.toModel().isMasterSigner) {
                            Timber.tag("miniscript-feature").d("PushEvent: Handling key_x_y pattern for master signer")
                            addMasterSignerToRelatedKeysInGroup(singleSigner.toModel(), keyName)
                        } else {
                            Timber.tag("miniscript-feature").d("PushEvent: Calling addSignerToGroupWithKeyName for single key")
                            addSignerToGroupWithKeyName(singleSigner, keyName)
                        }
                        
                        _uiState.update { state -> state.copy(currentKeyToAssign = "") }
                        // Reload signers to include the newly created one
                        loadSigners()
                    } else {
                    addSignerToGroup(it.signer)
                        // Reload signers to include the newly created one
                        loadSigners()
                    }
                }
        }
        viewModelScope.launch {
            _uiState.mapNotNull { it.group?.addressType }.distinctUntilChanged()
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
        viewModelScope.launch {
            getGroupDeviceUIDUseCase(Unit).onSuccess { uid ->
                deviceUID = uid
            }
        }
        viewModelScope.launch {
            while (isActive) {
                uiState.value.group?.let { group ->
                    updateOccupiedSlots(group)
                }
                delay(30.seconds.inWholeMilliseconds)
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

    private fun listenGroupOnline() {
        viewModelScope.launch {
            GroupOnlineListener.getGroupFlow().collect { groupOnline ->
                if (groupOnline.groupId == groupId) {
                    _uiState.update { it.copy(numberOfOnlineUsers = groupOnline.online) }
                }
            }
        }
    }

    private fun listenGroupSandbox() {
        groupUpdateJob = viewModelScope.launch {
            GroupSandboxListener.getGroupFlow().collect { groupSandbox ->
                Timber.d("GroupSandboxListener $groupSandbox")
                if (groupSandbox.id == groupId) {
                    if (groupSandbox.finalized) {
                        _uiState.update { it.copy(finalizedWalletId = groupSandbox.walletId) }
                    } else {
                        updateGroupSandbox(groupSandbox)
                    }
                }
            }
        }
    }

    private fun listenGroupDelete() {
        viewModelScope.launch {
            GroupDeleteListener.groupDeleteFlow.collect { it ->
                Timber.d("GroupDeleteListener $it")
                if (it == groupId) {
                    _uiState.update { it.copy(groupWalletUnavailable = true) }
                }
            }
        }
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
                    .map { signer ->
                        masterSignerMapper(signer)
                    } + singleSigner.map { signer -> signer.toModel() }
                _uiState.update { it.copy(allSigners = signers) }
            }
        }
    }

    fun createGroupSandbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val numOfGroups = getGroupWalletsUseCase(Unit).getOrNull()?.size ?: 0
            val groupName = if (numOfGroups == 0) {
                "Group wallet"
            } else {
                "Group wallet #${numOfGroups + 1}"
            }
            
            val currentState = _uiState.value
            val scriptTemplate = if (currentState.miniscriptTemplate.isNotEmpty()) currentState.miniscriptTemplate else null
            
            createGroupSandboxUseCase(
                CreateGroupSandboxUseCase.Params(
                    groupName,
                    2,
                    3,
                    AddressType.NATIVE_SEGWIT,
                    scriptTemplate = scriptTemplate
                )
            ).onSuccess { groupSandbox ->
                savedStateHandle[FreeGroupWalletActivity.EXTRA_GROUP_ID] = groupSandbox.id
                updateGroupSandbox(groupSandbox)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun getGroupOnline() {
        viewModelScope.launch {
            getGroupOnlineUseCase(groupId).onSuccess { groupOnline ->
                _uiState.update { it.copy(numberOfOnlineUsers = groupOnline) }
            }
        }
    }

    fun getGroupSandbox() {
        if (groupId.isNotEmpty()) {
            Timber.d("Get group sandbox $groupId")
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                getGroupSandboxUseCase(groupId).onSuccess { groupSandbox ->
                    savedStateHandle[FreeGroupWalletActivity.EXTRA_REPLACE_WALLET_ID] = groupSandbox.replaceWalletId
                    updateGroupSandbox(groupSandbox)
                }.onFailure {
                    Timber.e("Failed to get group sandbox $it")
                    if (it is NCNativeException && it.message.contains("-7008")) {
                        Timber.d("Group not found, finish screen")
                        _uiState.update { state -> state.copy(isFinishScreen = true) }
                    }
                }
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun updateGroupSandbox(groupSandbox: GroupSandbox) {
        Timber.tag("miniscript-feature").d("Update group sandbox: $groupSandbox")
        
        getSignerOldWallet(groupSandbox.replaceWalletId)
        val signers = mapSigners(groupSandbox.signers)
        
        // Load miniscript script node if this is a miniscript wallet
        if (groupSandbox.walletType == WalletType.MINISCRIPT && groupSandbox.miniscriptTemplate.isNotEmpty()) {
            loadMiniscriptScriptNode(groupSandbox.miniscriptTemplate, signers, groupSandbox)
        }
        
        // Extract namedOccupied key names for Miniscript wallets
        val namedOccupiedKeys = if (groupSandbox.walletType == WalletType.MINISCRIPT) {
            val keys = groupSandbox.namedOccupied.keys.toSet()
            Timber.tag("miniscript-feature").d("Extracted namedOccupied keys: $keys from data: ${groupSandbox.namedOccupied}")
            keys
        } else {
            emptySet()
        }
        
        if (replaceWalletId.isNotEmpty()) {
            _uiState.update { it.copy(
                group = groupSandbox, 
                replaceSigners = signers, 
                isInReplaceMode = true,
                miniscriptTemplate = groupSandbox.miniscriptTemplate,
                namedOccupied = namedOccupiedKeys
            ) }
        } else {
            _uiState.update { it.copy(
                group = groupSandbox, 
                signers = signers, 
                isInReplaceMode = false,
                miniscriptTemplate = groupSandbox.miniscriptTemplate,
                namedOccupied = namedOccupiedKeys
            ) }
        }
        updateOccupiedSlots(groupSandbox)
    }

    private suspend fun loadMiniscriptScriptNode(miniscriptTemplate: String, signers: List<SignerModel?>, groupSandbox: GroupSandbox) {
        getScriptNodeFromMiniscriptTemplateUseCase(miniscriptTemplate).onSuccess { result ->
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
            
            // Create signer map from named signers
            val signerMap = createSignerMapFromNamedSigners(signers, result.scriptNode, groupSandbox)
            
            _uiState.update { 
                it.copy(
                    scriptNode = result.scriptNode,
                    keyPath = result.keyPath,
                    scriptNodeMuSig = scriptNodeMuSig,
                    namedSigners = signerMap
                )
            }
            
        }.onFailure { error ->
            Timber.tag("miniscript-feature").e("Failed to load miniscript script node: $error")
        }
    }

    private fun createSignerMapFromNamedSigners(signers: List<SignerModel?>, scriptNode: ScriptNode, groupSandbox: GroupSandbox): Map<String, SignerModel?> {
        val signerMap = mutableMapOf<String, SignerModel?>()
        
        if (groupSandbox.walletType == WalletType.MINISCRIPT && groupSandbox.namedSigners.isNotEmpty()) {
            // Use the namedSigners from GroupSandbox directly and convert them to SignerModel
            // This preserves the correct BIP32 path for each specific key
            groupSandbox.namedSigners.forEach { (keyName, singleSigner) ->
                // Check if the SingleSigner actually has meaningful data (not empty/null)
                if (singleSigner.masterFingerprint.isNotEmpty() && singleSigner.xpub.isNotEmpty()) {
                    // Find the corresponding signer in the signers list to get the correct name
                    val existingSigner = signers.find { it?.fingerPrint == singleSigner.masterFingerprint }
                    
                    // Convert SingleSigner to SignerModel, but use the correct name from existing signer if available
                    val signerModel = if (existingSigner != null && existingSigner.name.isNotEmpty()) {
                        singleSigner.toModel().copy(name = existingSigner.name)
                    } else {
                        singleSigner.toModel()
                    }
                    
                    signerMap[keyName] = signerModel
                    
                    Timber.tag("miniscript-feature").d("createSignerMapFromNamedSigners: key=$keyName, path=${signerModel.derivationPath}, name=${signerModel.name}")
                } else {
                    // This key doesn't have a signer assigned yet, set to null
                    signerMap[keyName] = null
                    Timber.tag("miniscript-feature").d("createSignerMapFromNamedSigners: key=$keyName has no signer assigned")
                }
            }
        } else {
            // Fallback: Extract key names from the script node and map by index
            val keyNames = extractKeyNamesFromScriptNode(scriptNode)
            keyNames.forEachIndexed { index, keyName ->
                if (index < signers.size) {
                    signerMap[keyName] = signers[index]
                } else {
                    signerMap[keyName] = null
                }
            }
        }
        
        Timber.tag("miniscript-feature").d("Created signer map: $signerMap")
        return signerMap
    }

    private fun extractKeyNamesFromScriptNode(node: ScriptNode): List<String> {
        val keyNames = mutableListOf<String>()
        
        keyNames.addAll(node.keys)
        
        node.subs.forEach { subNode ->
            keyNames.addAll(extractKeyNamesFromScriptNode(subNode))
        }
        
        return keyNames
    }

    private suspend fun mapSigners(signers: List<SingleSigner>) =
        signers.mapIndexed { index, groupSigner ->
            groupSigner.takeIf { it.masterFingerprint.isNotEmpty() || it.name == KEY_NOT_SYNCED_NAME }
                ?.let { signer ->
                    if (hasSignerUseCase(signer).getOrNull() == true) {
                        singleSignerMapper(getSignerUseCase(signer).getOrThrow()).copy(isVisible = true)
                    } else if (groupSigner.name == KEY_NOT_SYNCED_NAME) {
                        groupSigner.toModel().copy(isVisible = false)
                    } else {
                        groupSigner.toModel().copy(isVisible = false, name = "Key #${index.inc()}")
                    }
                }
        }

    private fun updateOccupiedSlots(groupSandbox: GroupSandbox) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000
        val timeout = 5.minutes.inWholeSeconds
        
        if (isMiniscriptWallet()) {
            val namedOccupiedKeys = groupSandbox.namedOccupied.mapNotNull { (keyName, occupiedData) ->
                // occupiedData is a tuple (deviceId, timestamp) - we need to check if it's from another device and not expired
                val deviceId = occupiedData.first.toString()
                val timestamp = occupiedData.second.toLongOrDefault(0)
                if (deviceId != deviceUID && timestamp + timeout > currentTimeInSeconds) {
                    keyName
                } else {
                    null
                }
            }.toSet()
            
            Timber.tag("miniscript-feature").d("updateOccupiedSlots: Miniscript wallet - namedOccupied keys: $namedOccupiedKeys")
            
            _uiState.update { it.copy(
                occupiedSlotsIndex = emptySet(),
                namedOccupied = namedOccupiedKeys
            ) }
        } else {
            // For regular wallets, handle occupiedSlots array
            val occupiedSlots =
                groupSandbox.occupiedSlots.mapIndexedNotNull { index, occupiedSlot ->
                    if (occupiedSlot != null && occupiedSlot.deviceId.toString() != deviceUID && occupiedSlot.time + timeout > currentTimeInSeconds) {
                        index
                    } else {
                        null
                    }
                }.toSet()
            
            Timber.tag("miniscript-feature").d("updateOccupiedSlots: Regular wallet - occupied slots: $occupiedSlots")
            
            _uiState.update { it.copy(
                occupiedSlotsIndex = occupiedSlots,
                namedOccupied = emptySet()
            ) }
        }
    }

    fun setCurrentSignerIndex(index: Int) {
        savedStateHandle[CURRENT_SIGNER_INDEX] = index
    }

    fun setCurrentSigner(signer: SignerModel) {
        savedStateHandle[CURRENT_SIGNER] = signer
    }

    fun addSignerToGroup(signer: SingleSigner) {
        addSignerJob = viewModelScope.launch {
            val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: return@launch
            Timber.d("Add signer to group $signer at index $index")
            val existingSigners = getWalletSigners()
            if (existingSigners.any { it.fingerPrint == signer.masterFingerprint }) {
                _uiState.update { it.copy(errorMessage = application.getString(R.string.nc_key_already_in_wallet)) }
                return@launch
            }
            addSignerToGroup(signer, index)
        }
    }

    private suspend fun addSignerToGroup(signer: SingleSigner, index: Int) {
        _uiState.update { it.copy(isLoading = true) }
        addSignerToGroupUseCase(
            AddSignerToGroupUseCase.Params(
                groupId,
                signer,
                index
            )
        ).onSuccess {
            updateGroupSandbox(it)
        }.onFailure { error ->
            Timber.e("Failed to add signer to group $error")
            _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun removeSignerFromGroup(index: Int) {
        viewModelScope.launch {
            val isInReplace = wallet != null
            if (isInReplace) {
                wallet?.signers?.getOrNull(index)?.let {
                    addSignerToGroup(it, index)
                }
            } else {
                _uiState.update { it.copy(isLoading = true) }
                removeSignerFromGroupUseCase(
                    RemoveSignerFromGroupUseCase.Params(
                        groupId,
                        index
                    )
                ).onSuccess {
                    updateGroupSandbox(it)
                }.onFailure { error ->
                    Timber.e("Failed to remove signer from group $error")
                    _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteGroupSandbox() {
        viewModelScope.launch {
            deleteGroupSandboxUseCase(groupId).onSuccess {
                _uiState.update { it.copy(isFinishScreen = true, group = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
            }
        }
    }

    fun markMessageHandled() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    fun getSuggestedSigners(): List<SupportedSigner> {
        return _uiState.value.let { state ->
            state.supportedTypes.takeIf { state.group?.addressType?.isTaproot() == true }
                ?.filter { it.walletType == WalletType.MULTI_SIG || it.walletType == null }.orEmpty()
        }
    }

    fun addExistingSigner(signer: SignerModel) {
        val addressType = _uiState.value.group?.addressType ?: return
        _uiState.update { it.copy(isLoading = true) }
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
                    addSignerToGroup(singleSigner)
                }.onFailure { error ->
                    Timber.e("Failed to add signer to group $error")
                    _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
                }
            } else {
                val singleSigner = singleSigners.find {
                    it.masterFingerprint == signer.fingerPrint &&
                            it.derivationPath == signer.derivationPath
                } ?: return@launch
                addSignerToGroup(singleSigner)
            }
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun setSlotOccupied(value: Boolean) {
        if (addSignerJob?.isActive == true) return
        viewModelScope.launch {
            if (isMiniscriptWallet()) {
                if (value) {
                    // Setting slot as occupied - use the current key being assigned
                    val currentKey = _uiState.value.currentKeyToAssign
                    if (currentKey.isNotEmpty()) {
                        setSlotOccupiedUseCase(
                            SetSlotOccupiedUseCase.Params(
                                groupId = groupId,
                                index = -1, // Use -1 for Miniscript since we use keyName
                                value = value,
                                keyName = currentKey
                            )
                        ).onSuccess {
                            Timber.tag("miniscript-feature").d("Miniscript slot for key '$currentKey' is occupied: $value")
                        }.onFailure {
                            Timber.tag("miniscript-feature").e("Failed to set Miniscript slot occupied for key '$currentKey': $it")
                        }
                    } else {
                        Timber.tag("miniscript-feature").d("No current key to assign, skipping slot occupied setting")
                    }
                } else {
                    // Clearing slot occupied - only clear if we have a current key or if we're in a state where we need to clear
                    val currentKey = _uiState.value.currentKeyToAssign
                    if (currentKey.isNotEmpty()) {
                        // Clear slot occupied for the specific key that was being assigned
                        setSlotOccupiedUseCase(
                            SetSlotOccupiedUseCase.Params(
                                groupId = groupId,
                                index = -1,
                                value = false,
                                keyName = currentKey
                            )
                        ).onSuccess {
                            Timber.tag("miniscript-feature").d("Cleared slot occupied for Miniscript key: $currentKey")
                        }.onFailure {
                            Timber.tag("miniscript-feature").e("Failed to clear slot occupied for Miniscript key '$currentKey': $it")
                        }
                    } else {
                        Timber.tag("miniscript-feature").d("No current key to clear, skipping slot occupied clearing")
                    }
                }
            } else {
                // For non-Miniscript wallets, use the numeric index
                val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: return@launch
                setSlotOccupiedUseCase(
                    SetSlotOccupiedUseCase.Params(
                        groupId = groupId,
                        index = index,
                        value = value
                    )
                ).onSuccess {
                    Timber.d("Slot $index is occupied: $value")
                }.onFailure {
                    Timber.e("Failed to set slot occupied $it")
                }
            }
        }
    }
    


    fun changeBip32Path(masterSignerId: String, newPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Params(
                    masterSignerId, newPath
                )
            ).onSuccess {
                val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: return@launch
                addSignerToGroup(it, index)
            }.onFailure { error ->
                if (error is NCNativeException && error.message.contains("-1009")) {
                    savedStateHandle[NEW_PATH] = newPath
                    _uiState.update { it.copy(requestCacheTapSignerXpubEvent = true) }
                } else {
                    Timber.e("Failed to change bip32 path $error")
                    _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun resetRequestCacheTapSignerXpub() {
        _uiState.update { it.copy(requestCacheTapSignerXpubEvent = false) }
    }

    fun cacheTapSignerXpub(isoDep: IsoDep?, cvc: String) {
        // Check if this is for Miniscript flow (pending state exists)
        if (pendingAddSignerState != null) {
            Timber.tag("miniscript-feature").d("Handling TapSigner caching for Miniscript flow")
            handleMiniscriptTapSignerCaching(isoDep, cvc)
            return
        }
        
        // Original flow for non-Miniscript operations
        val signer = savedStateHandle.get<SignerModel>(CURRENT_SIGNER) ?: return
        val newPath = savedStateHandle.get<String>(NEW_PATH) ?: return
        Timber.d("Cache tap signer xpub $signer $newPath")
        isoDep ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getSignerFromTapsignerMasterSignerByPathUseCase(
                GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                    isoDep = isoDep,
                    masterSignerId = signer.id,
                    path = newPath,
                    cvc = cvc
                )
            ).onSuccess {
                Timber.d("new signer $it")
                addSignerToGroup(it)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message.orUnknownError(),
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private fun handleMiniscriptTapSignerCaching(isoDep: IsoDep?, cvc: String) {
        val pendingState = pendingAddSignerState ?: return
        val signerModel = pendingState.signerModel
        val newPath = savedStateHandle.get<String>(NEW_PATH) ?: return
        
        Timber.tag("miniscript-feature").d("Handling Miniscript TapSigner caching for signer: ${signerModel.name} with path: $newPath")
        isoDep ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // For Miniscript, we need to cache the XPUB using the stored path
            getSignerFromTapsignerMasterSignerByPathUseCase(
                GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                    isoDep = isoDep,
                    masterSignerId = signerModel.fingerPrint,
                    path = newPath,
                    cvc = cvc
                )
            ).onSuccess { newSigner ->
                Timber.tag("miniscript-feature").d("Successfully cached TapSigner XPUB: ${newSigner.name}")
                
                // Resume the add signer process
                resumeAddSignerProcess()
            }.onFailure { error ->
                Timber.tag("miniscript-feature").e("Failed to cache TapSigner XPUB: $error")
                _uiState.update {
                    it.copy(
                        errorMessage = error.message.orUnknownError(),
                        isLoading = false,
                        requestCacheTapSignerXpubEvent = false
                    )
                }
                pendingAddSignerState = null
            }
        }
    }

    fun createReplaceGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            createReplaceGroupUseCase(replaceWalletId).onSuccess {
                savedStateHandle[FreeGroupWalletActivity.EXTRA_GROUP_ID] = it.id
                updateGroupSandbox(it)
                getSignerOldWallet(replaceWalletId)
                _uiState.update { state -> state.copy(isCreatedReplaceGroup = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun getSignerOldWallet(walletId: String) {
        if (walletId.isEmpty()) return
        if (wallet != null) return
        getWalletDetail2UseCase(walletId).onSuccess { wallet ->
            this.wallet = wallet
            val signers = mapSigners(wallet.signers)
            _uiState.update { it.copy(signers = signers) }
        }
    }

    fun resetReplaceGroup() {
        _uiState.update { it.copy(isCreatedReplaceGroup = false) }
    }

    fun finalizeGroup(group: GroupSandbox) {
        viewModelScope.launch {
            groupUpdateJob?.cancel()
            finalizeGroupSandboxUseCase(
                FinalizeGroupSandboxUseCase.Params(
                    groupId = group.id,
                    signerIndexes = if (group.addressType.isTaproot()) (0 until group.m).toSet() else emptySet()
                )
            ).onSuccess { group ->
                _uiState.update { it.copy(finalizeGroup = group, group = group) }
            }.onFailure { error ->
                // listen group sandbox again
                listenGroupSandbox()
                _uiState.update { it.copy(errorMessage = error.message.orUnknownError()) }
            }
        }
    }

    fun getWallet() = wallet

    fun markFinalizeGroupHandled() {
        _uiState.update { it.copy(finalizeGroup = null) }
    }

    fun getWalletSigners(): List<SignerModel> = _uiState.value.signers.filterNotNull()

    // Key action methods for Miniscript
    fun setCurrentKeyToAssign(keyName: String) {
        _uiState.update { it.copy(currentKeyToAssign = keyName) }
    }

    fun addExistingSignerForKey(signer: SignerModel, keyName: String) {
        Timber.tag("miniscript-feature").d("addExistingSignerForKey called: signer=${signer.name}(${signer.fingerPrint}), keyName=$keyName")
        
        val addressType = _uiState.value.group?.addressType
        if (addressType == null) {
            return
        }

        // Check if this signer fingerprint is already used in other keys
        val isSignerAlreadyUsed = checkIfSignerAlreadyUsed(signer.fingerPrint, keyName, signer.isMasterSigner)
        if (isSignerAlreadyUsed) {
        _uiState.update {
                it.copy(
                    event = FreeGroupWalletEvent.ShowDuplicateSignerWarning(signer, keyName)
                ) 
            }
            return
        }

        Timber.tag("miniscript-feature").d("addExistingSignerForKey: Proceeding with adding signer")
        proceedWithAddingSignerForKey(signer, keyName)
    }
    
        private fun proceedWithAddingSignerForKey(signer: SignerModel, keyName: String) {
        Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey called: signer=${signer.name}(${signer.fingerPrint}), keyName=$keyName, isMasterSigner=${signer.isMasterSigner}")
        
        val addressType = _uiState.value.group?.addressType
        if (addressType == null) {
            Timber.tag("miniscript-feature").e("proceedWithAddingSignerForKey: addressType is null, returning")
            return
        }
        
        _uiState.update { it.copy(event = FreeGroupWalletEvent.Loading(true)) }
        viewModelScope.launch {
            if (signer.isMasterSigner) {
                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Processing master signer")
                val masterSigner = masterSigners.find { it.id == signer.fingerPrint }
                if (masterSigner == null) {
                    Timber.tag("miniscript-feature").e("proceedWithAddingSignerForKey: Master signer not found with id=${signer.fingerPrint}")
                    _uiState.update { it.copy(event = FreeGroupWalletEvent.Error("Master signer not found")) }
                    return@launch
                }
                
                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Found master signer: ${masterSigner.name}")
                getUnusedSignerFromMasterSignerV2UseCase(
                    GetUnusedSignerFromMasterSignerV2UseCase.Params(
                        masterSigner,
                        WalletType.MULTI_SIG,
                        addressType
                    )
                ).onSuccess { singleSigner ->
                    Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Got unused signer $singleSigner")
                    // For Miniscript wallets, check if signer already exists in group
                    if (isMiniscriptWallet()) {
                        val existingSignerInGroup = _uiState.value.group?.signers?.find { 
                            it.masterFingerprint == singleSigner.masterFingerprint 
                        }
                        
                        if (existingSignerInGroup != null) {
                            Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Signer already exists in group")
                            // Check if this is key_x_y pattern with master signer
                            if (isKeyPatternXY(keyName) && singleSigner.toModel().isMasterSigner) {
                                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Handling key_x_y pattern for existing signer")
                                addMasterSignerToRelatedKeysInGroup(singleSigner.toModel(), keyName)
                            } else {
                                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Updating local state only for existing signer")
                                addSignerToStateForKey(singleSigner.toModel(), keyName)
                            }
                        } else {
                            // Check if this is key_x_y pattern with master signer
                            if (isKeyPatternXY(keyName) && singleSigner.toModel().isMasterSigner) {
                                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Handling key_x_y pattern for new signer")
                                addMasterSignerToRelatedKeysInGroup(singleSigner.toModel(), keyName)
                            } else {
                                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Calling addSignerToGroupWithKeyName for new signer")
                                addSignerToGroupWithKeyName(singleSigner, keyName)
                            }
                        }
                    } else {
                        Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Calling addSignerToStateForKey for non-Miniscript wallet")
                        addSignerToStateForKey(singleSigner.toModel(), keyName)
                    }
                }.onFailure { error ->
                    Timber.tag("miniscript-feature").e("proceedWithAddingSignerForKey: Failed to get unused signer: $error")
                    _uiState.update { it.copy(event = FreeGroupWalletEvent.Error(error.message.orUnknownError())) }
                }
            } else {
                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Processing single signer")
                val singleSigner = singleSigners.find {
                    it.masterFingerprint == signer.fingerPrint &&
                            it.derivationPath == signer.derivationPath
                }
                if (singleSigner == null) {
                    Timber.tag("miniscript-feature").e("proceedWithAddingSignerForKey: Single signer not found with fingerPrint=${signer.fingerPrint}, derivationPath=${signer.derivationPath}")
                    _uiState.update { it.copy(event = FreeGroupWalletEvent.Error("Single signer not found")) }
                    return@launch
                }
                
                Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Found single signer: ${singleSigner.name}")
                // For Miniscript wallets, check if signer already exists in group
                if (isMiniscriptWallet()) {
                    val existingSignerInGroup = _uiState.value.group?.signers?.find { 
                        it.masterFingerprint == singleSigner.masterFingerprint 
                    }
                    
                    if (existingSignerInGroup != null) {
                        Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Single signer already exists in group, updating local state only")
                        addSignerToStateForKey(singleSigner.toModel(), keyName)
                    } else {
                        Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Calling addSignerToGroupWithKeyName for new single signer")
                        addSignerToGroupWithKeyName(singleSigner, keyName)
                    }
                } else {
                    Timber.tag("miniscript-feature").d("proceedWithAddingSignerForKey: Calling addSignerToStateForKey for non-Miniscript wallet")
                    addSignerToStateForKey(singleSigner.toModel(), keyName)
                }
            }
        }
        _uiState.update { it.copy(event = FreeGroupWalletEvent.Loading(false)) }
    }
    
    private fun checkIfSignerAlreadyUsed(fingerPrint: String, excludeKeyName: String, isMasterSigner: Boolean): Boolean {
        val currentSigners = _uiState.value.namedSigners
        
        // For master signers with key pattern key_x_y, check if the fingerprint is used outside the related keys group
        if (isMasterSigner && isKeyPatternXY(excludeKeyName)) {
            val prefix = getKeyPrefix(excludeKeyName)
            val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                .filter { it.startsWith("${prefix}_") }

            // Check if fingerprint is used in keys outside the related group
            val isUsedOutsideRelatedKeys = currentSigners.any { (keyName, signerModel) ->
                val isOutsideRelatedKeys = !relatedKeys.contains(keyName) && signerModel?.fingerPrint == fingerPrint
                isOutsideRelatedKeys
            }
            
            return isUsedOutsideRelatedKeys
        }
        
        // For non-master signers or keys that don't follow key_x_y pattern, use original logic
        val isUsedInRegularKeys = currentSigners.any { (keyName, signerModel) ->
            keyName != excludeKeyName && signerModel?.fingerPrint == fingerPrint
        }
        
        return isUsedInRegularKeys
    }
    
    private fun isKeyPatternXY(keyName: String): Boolean {
        val components = keyName.split("_")
        val isPattern = components.size >= 3 && components[0] == "key" && 
               components[1].toIntOrNull() != null && components[2].toIntOrNull() != null
        
        Timber.tag("miniscript-feature").d("isKeyPatternXY: keyName=$keyName, components=$components, isPattern=$isPattern")
        return isPattern
    }
    
    private fun getKeyPrefix(keyName: String): String {
        val components = keyName.split("_")
        val prefix = if (components.size >= 3 && components[0] == "key") "${components[0]}_${components[1]}" else keyName
        
        Timber.tag("miniscript-feature").d("getKeyPrefix: keyName=$keyName, components=$components, prefix=$prefix")
        return prefix
    }
    
    private fun getAllKeysFromScriptNode(node: ScriptNode): Set<String> {
        val keys = mutableSetOf<String>()
        
        Timber.tag("miniscript-feature").d("getAllKeysFromScriptNode: Processing node with keys: ${node.keys}")
        
        // Add keys from current node
        keys.addAll(node.keys)
        
        // Recursively add keys from sub-nodes
        node.subs.forEach { subNode ->
            keys.addAll(getAllKeysFromScriptNode(subNode))
        }
        return keys
    }
    
    private fun addSignerToStateForKey(signerModel: SignerModel, keyName: String) {
        Timber.tag("miniscript-feature").d("addSignerToStateForKey called: signer=${signerModel.name}(${signerModel.fingerPrint}), keyName=$keyName, isMasterSigner=${signerModel.isMasterSigner}")
        
        val currentSigners = _uiState.value.namedSigners.toMutableMap()
        
        Timber.tag("miniscript-feature").d("addSignerToStateForKey: Current signers before change: $currentSigners")
        
        // Check if keyName matches the pattern key_x_y (e.g., key_0_0, key_0_1)
        if (isKeyPatternXY(keyName) && signerModel.isMasterSigner) {
            Timber.tag("miniscript-feature").d("addSignerToStateForKey: Handling master signer with key_x_y pattern for keyName=$keyName")
            // For key_x_y pattern with master signers, we need to handle it differently in group wallets
            // This should be called from a separate method, not here
            Timber.tag("miniscript-feature").w("addSignerToStateForKey: key_x_y pattern should use addMasterSignerToRelatedKeys method instead")
            
            // Fallback to simple assignment for now
            val previousSigner = currentSigners[keyName]
            currentSigners[keyName] = signerModel
            Timber.tag("miniscript-feature").d("addSignerToStateForKey: Added master signer to single key: $keyName (previous: ${previousSigner?.name})")
        } else {
            // Handle simple key names (A, B, C, D, E, etc.) or non-master signers
            val previousSigner = currentSigners[keyName]
            currentSigners[keyName] = signerModel
            Timber.tag("miniscript-feature").d("addSignerToStateForKey: Added signer to key: $keyName (previous: ${previousSigner?.name})")
        }
        
        Timber.tag("miniscript-feature").d("addSignerToStateForKey: Current signers after change: $currentSigners")
        
        _uiState.update { 
            it.copy(
                namedSigners = currentSigners,
                event = FreeGroupWalletEvent.SignerAdded(keyName, signerModel)
            )
        }
        Timber.tag("miniscript-feature").d("addSignerToStateForKey: Updated state after adding signer(s). Current signers: $currentSigners")
    }
    
    fun removeSignerForKey(keyName: String) {
        Timber.tag("miniscript-feature").d("Removing signer for key: $keyName")
        
        viewModelScope.launch {
            // For Miniscript wallets, call removeSignerFromGroup with keyName
            if (isMiniscriptWallet()) {
                // Check if this is key_x_y pattern with master signer
                val currentSigners = _uiState.value.namedSigners
                val signerToRemove = currentSigners[keyName]
                
                if (isKeyPatternXY(keyName) && signerToRemove?.fingerPrint?.isNotEmpty() == true) {
                    Timber.tag("miniscript-feature").d("removeSignerForKey: Handling key_x_y pattern for master signer removal")
                    // signerToRemove is already a SignerModel, use it directly
                    removeMasterSignerFromRelatedKeysInGroup(signerToRemove, keyName)
                } else {
                    Timber.tag("miniscript-feature").d("removeSignerForKey: Calling removeSignerFromGroupWithKeyName for single key")
                    removeSignerFromGroupWithKeyName(keyName)
                }
            } else {
                // Legacy behavior for non-Miniscript wallets
                removeSignerFromStateForKey(keyName)
            }
        }
    }
    
    private suspend fun removeSignerFromGroupWithKeyName(keyName: String) {
        _uiState.update { it.copy(isLoading = true) }
        removeSignerFromGroupUseCase(
            RemoveSignerFromGroupUseCase.Params(
                groupId = groupId,
                index = -1, // Use -1 for Miniscript since we use keyName instead of index
                keyName = keyName
            )
        ).onSuccess {
            updateGroupSandbox(it)
            _uiState.update { state -> state.copy(event = FreeGroupWalletEvent.SignerRemoved(keyName)) }
        }.onFailure { error ->
            Timber.e("Failed to remove signer from group $error")
            _uiState.update { it.copy(event = FreeGroupWalletEvent.Error(error.message.orUnknownError())) }
        }
        _uiState.update { it.copy(isLoading = false) }
    }
    
    private fun removeSignerFromStateForKey(keyName: String) {
        val currentSigners = _uiState.value.namedSigners.toMutableMap()

        Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Removing signer for keyName=$keyName")

        // Check if keyName matches the pattern key_x_y (e.g., key_0_0, key_0_1)
        if (isKeyPatternXY(keyName)) {
            Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Handling key with pattern key_x_y")
            // Extract the prefix (key_x)
            val prefix = getKeyPrefix(keyName)

            // Get the signer to remove
            val signerToRemove = currentSigners[keyName]
            Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Signer to remove: $signerToRemove")

            // If it's a master signer, remove all related keys with the same master fingerprint
            if (signerToRemove?.isMasterSigner == true) {
                val masterFingerprintToRemove = signerToRemove.fingerPrint
                Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Removing all related keys with master fingerprint: $masterFingerprintToRemove")

                // Find all keys in scriptNode that share the same prefix (key_x_*)
                val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
                    .filter { it.startsWith("${prefix}_") }

                Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Found related keys to remove: $relatedKeys")

                // Remove signers for all related keys with the same master fingerprint
                relatedKeys.forEach { key ->
                    if (currentSigners[key]?.fingerPrint == masterFingerprintToRemove) {
                        currentSigners[key] = null
                        Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Removed signer for key: $key")
                    }
                }
            } else {
                // For non-master signers, just remove the specific key
                currentSigners[keyName] = null
                Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Removed non-master signer for key: $keyName")
            }
        } else {
            // Handle simple key names (A, B, C, D, E, etc.)
            currentSigners[keyName] = null
            Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Removed signer for simple key: $keyName")
        }

        _uiState.update {
            it.copy(
                namedSigners = currentSigners,
                event = FreeGroupWalletEvent.SignerRemoved(keyName)
            )
        }
        Timber.tag("miniscript-feature").d("removeSignerFromStateForKey: Updated state after removing signer(s). Current signers: $currentSigners")
    }
    
    fun markEventHandled() {
        _uiState.update { it.copy(event = null) }
    }
    
    private fun isMiniscriptWallet(): Boolean {
        return _uiState.value.group?.walletType == WalletType.MINISCRIPT
    }
    

    
    private suspend fun addMasterSignerToRelatedKeysInGroup(signerModel: SignerModel, keyName: String) {
        Timber.tag("miniscript-feature").d("addMasterSignerToRelatedKeysInGroup called: signer=${signerModel.name}(${signerModel.fingerPrint}), keyName=$keyName")
        
        val prefix = getKeyPrefix(keyName)
        val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
            .filter { it.startsWith("${prefix}_") }
            .sorted() // Sort to ensure consistent order
            
        Timber.tag("miniscript-feature").d("addMasterSignerToRelatedKeysInGroup: Found related keys: $relatedKeys")
        
        // Get the current index from master signer
        getCurrentIndexFromMasterSignerUseCase(
            GetCurrentIndexFromMasterSignerUseCase.Param(
                xfp = signerModel.fingerPrint,
                walletType = WalletType.MULTI_SIG,
                addressType = _uiState.value.group?.addressType ?: AddressType.TAPROOT
            )
        ).onSuccess { startIndex ->
            val actualStartIndex = if (startIndex == -1) 0 else startIndex
            Timber.tag("miniscript-feature").d("addMasterSignerToRelatedKeysInGroup: Got start index: $actualStartIndex")
            
            // For each related key, get a signer with increasing index using GetSignerFromMasterSignerByIndexUseCase
            var currentIndex = actualStartIndex
            var shouldReturn = false
            
            for (key in relatedKeys) {
                if (shouldReturn) break
                
                Timber.tag("miniscript-feature").d("addMasterSignerToRelatedKeysInGroup: Processing key=$key with index=$currentIndex")
                
                try {
                    val singleSigner = getSignerFromMasterSignerByIndexUseCase(
                        GetSignerFromMasterSignerByIndexUseCase.Param(
                            masterSignerId = signerModel.fingerPrint,
                            index = currentIndex,
                            walletType = WalletType.MULTI_SIG,
                            addressType = _uiState.value.group?.addressType ?: AddressType.TAPROOT
                        )
                    ).getOrThrow()
                    
                    singleSigner?.let {
                        Timber.tag("miniscript-feature").d("addMasterSignerToRelatedKeysInGroup: Got signer for key=$key: ${it.name}")
                        
                        // Add this signer to the group with the specific key name
                        addSignerToGroupWithKeyName(it, key)
                    } ?: run {
                        Timber.tag("miniscript-feature").e("addMasterSignerToRelatedKeysInGroup: No signer found for key=$key at index=$currentIndex")
                    }
                } catch (error: Exception) {
                    Timber.tag("miniscript-feature").e("addMasterSignerToRelatedKeysInGroup: Failed to get signer for key=$key at index=$currentIndex: $error")
                    
                    // Handle TapSigner caching case
                    if (error is NCNativeException && error.message.contains("-1009")) {
                        Timber.tag("miniscript-feature").d("Handling TapSigner caching for key=$key at index=$currentIndex")
                        
                        val isMultisig = isMultisigDerivationPath(signerModel.derivationPath)
                        val newPath = getPath(currentIndex, _uiState.value.isTestNet, isMultisig)
                        
                        // Store the pending state for resuming after caching
                        pendingAddSignerState = PendingAddSignerState(
                            signerModel = signerModel,
                            keyName = keyName,
                            relatedKeys = relatedKeys,
                            currentIndex = currentIndex,
                            processedKeyIndex = relatedKeys.indexOf(key)
                        )
                        
                        // Store the path for caching
                        savedStateHandle[NEW_PATH] = newPath
                        
                        // Update UI state to request caching
                        _uiState.update { 
                            it.copy(
                                requestCacheTapSignerXpubEvent = true,
                                event = FreeGroupWalletEvent.Loading(false)
                            ) 
                        }
                        
                        shouldReturn = true
                        break
                    }
                }
                
                currentIndex++
            }
        }.onFailure { error ->
            Timber.tag("miniscript-feature").e("addMasterSignerToRelatedKeysInGroup: Failed to get current index: $error")
            _uiState.update { it.copy(event = FreeGroupWalletEvent.Error(error.message.orUnknownError())) }
        }
    }

    private suspend fun removeMasterSignerFromRelatedKeysInGroup(signerModel: SignerModel, keyName: String) {
        Timber.tag("miniscript-feature").d("removeMasterSignerFromRelatedKeysInGroup called: signer=${signerModel.name}(${signerModel.fingerPrint}), keyName=$keyName")
        
        val prefix = getKeyPrefix(keyName)
        val relatedKeys = getAllKeysFromScriptNode(_uiState.value.scriptNode!!)
            .filter { it.startsWith("${prefix}_") }
            .sorted() // Sort to ensure consistent order
            
        Timber.tag("miniscript-feature").d("removeMasterSignerFromRelatedKeysInGroup: Found related keys: $relatedKeys")
        
        // For each related key, remove the signer from the group
        relatedKeys.forEach { key ->
            removeSignerFromGroupWithKeyName(key)
        }
    }

    private suspend fun addSignerToGroupWithKeyName(signer: SingleSigner, keyName: String) {
        Timber.tag("miniscript-feature").d("addSignerToGroupWithKeyName called: signer=${signer.name}(${signer.masterFingerprint}), keyName=$keyName, groupId=$groupId")
        
        val params = AddSignerToGroupUseCase.Params(
            groupId = groupId,
            signer = signer,
            index = -1, // Use -1 for Miniscript since we use keyName instead of index
            keyName = keyName
        )
        
        Timber.tag("miniscript-feature").d("addSignerToGroupWithKeyName: Calling addSignerToGroupUseCase with params: $params")
        
        addSignerToGroupUseCase(params).onSuccess { groupSandbox ->
            Timber.tag("miniscript-feature").d("addSignerToGroupWithKeyName: Updated namedSigners: ${groupSandbox.namedSigners}")
            updateGroupSandbox(groupSandbox)
            _uiState.update { state -> state.copy(event = FreeGroupWalletEvent.SignerAdded(keyName, signer.toModel())) }
        }.onFailure { error ->
            Timber.tag("miniscript-feature").e("addSignerToGroupWithKeyName: Failed to add signer to group: $error")
            _uiState.update { it.copy(event = FreeGroupWalletEvent.Error(error.message.orUnknownError())) }
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

        Timber.tag("miniscript-feature").d("Resuming addMasterSignerToRelatedKeysInGroup process for key: ${pendingState.keyName}")

        // Resume the process from where it left off
        viewModelScope.launch {
            addMasterSignerToRelatedKeysInGroup(
                pendingState.signerModel, 
                pendingState.keyName
            )
        }
    }

    companion object {
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
        private const val CURRENT_SIGNER = "current_signer"
        private const val NEW_PATH = "new_path"
    }
}

// Data class for handling pending add signer operations during TapSigner caching
data class PendingAddSignerState(
    val signerModel: SignerModel,
    val keyName: String,
    val relatedKeys: List<String>,
    val currentIndex: Int,
    val processedKeyIndex: Int
)