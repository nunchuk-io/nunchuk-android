package com.nunchuk.android.main.groupwallet.recover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.utils.ParseSignerStringUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.miniscript.MiniscriptUtil
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.main.groupwallet.FreeGroupWalletActivity
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.GetScriptNodeFromMiniscriptTemplateUseCase
import com.nunchuk.android.usecase.free.groupwallet.RecoverGroupWalletUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletRecoverViewModel @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val singleSignerMapper: SingleSignerMapper,
    private val savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    private val recoverGroupWalletUseCase: RecoverGroupWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val pushEventManager: PushEventManager,
    private val getScriptNodeFromMiniscriptTemplateUseCase: GetScriptNodeFromMiniscriptTemplateUseCase,
    private val parseSignerStringUseCase: ParseSignerStringUseCase,
) : ViewModel() {
    val walletId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_WALLET_ID).orEmpty()
    val filePath: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_FILE_PATH).orEmpty()
    val qrList: List<String>
        get() = savedStateHandle.get<Array<String>>(FreeGroupWalletActivity.EXTRA_QR_LIST)?.toList()
            ?: emptyList()

    private val _uiState = MutableStateFlow(FreeGroupWalletRecoverUiState())
    val uiState: StateFlow<FreeGroupWalletRecoverUiState> = _uiState.asStateFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()

    init {
        loadInfo()

        viewModelScope.launch {
            pushEventManager.event.filterIsInstance<PushEvent.LocalUserSignerAdded>()
                .collect {
                    Timber.d("Pushing event: $it")
                    newSignerAdded(it.signer)
                }
        }
    }

    private fun loadSupportedSigners() = viewModelScope.launch {
        _uiState.mapNotNull { it.wallet?.addressType }.distinctUntilChanged()
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

    fun getSuggestedSigners(): List<SupportedSigner> {
        return _uiState.value.let { state ->
            state.supportedTypes.takeIf { state.wallet?.addressType?.isTaproot() == true }
                ?.filter { it.walletType == WalletType.MULTI_SIG || it.walletType == null }
                .orEmpty()
        }
    }

    fun getSuggestedSignersForMiniscript(): List<SupportedSigner> {
        return _uiState.value.supportedTypes.filter { 
            it.walletType == WalletType.MULTI_SIG || it.walletType == null 
        }
    }

    fun loadInfo() = viewModelScope.launch {
        val curWallet = _uiState.value.wallet
        val newWalletSigners: List<SignerModel>
        if (curWallet == null) {
            val getWalletDetail = async { getWalletDetail2UseCase(walletId) }
            val newWalletDetail = getWalletDetail.await().getOrNull() ?: return@launch
            newWalletSigners = newWalletDetail.signers.map { signer -> singleSignerMapper(signer) }
            _uiState.update { it.copy(wallet = newWalletDetail) }
            getMiniscriptInfo()
            deleteWallet()
        } else {
            newWalletSigners = curWallet.signers.map { signer -> singleSignerMapper(signer) }
        }

        val allSigners = getNewAllSigner()
        val oldAllSigners = _uiState.value.allSigners
        val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: -1
        val currentKey = savedStateHandle.get<String>(CURRENT_SIGNER_KEY) ?: ""
        Timber.tag(TAG)
            .e("Old all signers: ${oldAllSigners.size} - New all signers: ${allSigners.size}")
        
        // Only check for miniscript approach if wallet is loaded
        val isWaitingForSigner = if (_uiState.value.wallet != null && isMiniscriptWallet()) {
            currentKey.isNotEmpty()
        } else {
            index != -1
        }
        
        Timber.tag(TAG).e("loadInfo - isMiniscript: ${isMiniscriptWallet()}, isWaitingForSigner: $isWaitingForSigner, currentKey: $currentKey, index: $index")
        
        if (!isWaitingForSigner) {
            Timber.tag(TAG).e("Initial setup - no signer being added")

            val existingSigners = newWalletSigners.filter { signer ->
                allSigners.any { it.fingerPrint == signer.fingerPrint }
            }
            val notExistingSigners = newWalletSigners.filter { signer ->
                allSigners.none { it.fingerPrint == signer.fingerPrint }
            }
            val signerUis = mutableListOf<SignerModelRecoverUi>()
            signerUis.addAll(existingSigners.mapIndexed { index, signer ->
                SignerModelRecoverUi(signer = signer, index = index, isInDevice = true)
            })
            val signersSize = signerUis.size
            signerUis.addAll(notExistingSigners.mapIndexed { index, signer ->
                SignerModelRecoverUi(
                    signer = signer,
                    index = signersSize + index,
                    isInDevice = false
                )
            })
            _uiState.update { it.copy(signerUis = signerUis, allSigners = allSigners) }

            loadSupportedSigners()
        } else {
            if ((allSigners.isEmpty() && oldAllSigners.isEmpty()).not() && oldAllSigners.size != allSigners.size) {
                Timber.tag(TAG).e("All signers size is different - handling added signer")
                val newSigner = allSigners.find { signer ->
                    oldAllSigners.none { it.fingerPrint == signer.fingerPrint }
                }
                handleAddedSigner(newSigner, index)
                _uiState.update { it.copy(allSigners = allSigners) }
            }
        }
    }

    private fun newSignerAdded(addedSigner: SingleSigner) = viewModelScope.launch {
        val oldAllSigners = _uiState.value.allSigners
        val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: -1
        val currentKey = savedStateHandle.get<String>(CURRENT_SIGNER_KEY) ?: ""
        val allSigners = getNewAllSigner()
        
        // Only check for miniscript approach if wallet is loaded
        val isWaitingForSigner = if (_uiState.value.wallet != null && isMiniscriptWallet()) {
            currentKey.isNotEmpty()
        } else {
            index != -1
        }
        
        Timber.tag(TAG).e("newSignerAdded - isMiniscript: ${isMiniscriptWallet()}, isWaitingForSigner: $isWaitingForSigner, currentKey: $currentKey, index: $index")
        
        if ((allSigners.isEmpty() && oldAllSigners.isEmpty()).not() && isWaitingForSigner && oldAllSigners.size == allSigners.size) { // handle case add existing signer
            Timber.tag(TAG).e("All signers size is the same - handling existing signer addition")
            val newSigner = singleSignerMapper(addedSigner)
            handleAddedSigner(newSigner, index)
        }
    }

    private fun handleAddedSigner(newSigner: SignerModel?, index: Int) {
        newSigner?.let {
            if (isMiniscriptWallet()) {
                handleMiniscriptSignerAdded(newSigner)
            } else {
                val selectSigner = _uiState.value.signerUis.find { it.index == index }?.signer
                Timber.tag(TAG).e("New signer found: $it")
                if (selectSigner?.fingerPrint == newSigner.fingerPrint) {
                    Timber.tag(TAG).e("New signer is selected")
                    val signerUi =
                        SignerModelRecoverUi(signer = newSigner, index = index, isInDevice = true)
                    val signers = _uiState.value.signerUis.toMutableList()
                    signers[index] = signerUi
                    _uiState.update { state -> state.copy(signerUis = signers) }
                } else {
                    _uiState.update { state -> state.copy(showAddKeyErrorDialog = true) }
                }
            }
        }
        if (isMiniscriptWallet()) {
            setCurrentSignerKey("")
        } else {
            setCurrentSignerIndex(-1)
        }
    }

    private fun handleMiniscriptSignerAdded(newSigner: SignerModel) {
        val currentKey = savedStateHandle.get<String>(CURRENT_SIGNER_KEY) ?: ""
        if (currentKey.isEmpty()) return
        
        Timber.tag(TAG).e("Handling miniscript signer added for key: $currentKey")
        
        // Check signerMap
        val selectSigner = _uiState.value.signerMap[currentKey]
        if (selectSigner?.fingerPrint == newSigner.fingerPrint) {
            Timber.tag(TAG).e("New signer matches selected signer in signerMap")
            // Update signerMap with visible signer
            val updatedSignerMap = _uiState.value.signerMap.toMutableMap()
            updatedSignerMap[currentKey] = newSigner.copy(isVisible = true)
            
            // Update muSigSignerMap if it contains the key
            val updatedMuSigSignerMap = _uiState.value.muSigSignerMap.toMutableMap()
            if (updatedMuSigSignerMap.containsKey(currentKey)) {
                updatedMuSigSignerMap[currentKey] = newSigner.copy(isVisible = true)
            }
            
            _uiState.update { state -> 
                state.copy(
                    signerMap = updatedSignerMap,
                    muSigSignerMap = updatedMuSigSignerMap
                )
            }
        } else {
            _uiState.update { state -> state.copy(showAddKeyErrorDialog = true) }
        }
    }

    private suspend fun getNewAllSigner(): List<SignerModel> {
        return withContext(viewModelScope.coroutineContext) {
            val pairSigner =
                getAllSignersUseCase(false).getOrNull() ?: return@withContext emptyList()
            val singleSigner = pairSigner.second.filter { it.type != SignerType.SERVER }
            singleSigners.apply {
                clear()
                addAll(singleSigner)
            }
            masterSigners.apply {
                clear()
                addAll(pairSigner.first)
            }
            pairSigner.first.filter { it.type != SignerType.SERVER }
                .map { signer ->
                    masterSignerMapper(signer)
                } + singleSigner.map { signer -> signer.toModel() }
        }
    }

    fun recoverGroupWallet() {
        Timber.tag(TAG).e("Recover group wallet")
        viewModelScope.launch {
            recoverGroupWalletUseCase(
                RecoverGroupWalletUseCase.Params(
                    name = _uiState.value.wallet?.name.orEmpty(),
                    filePath = filePath,
                    qrList = qrList,
                )
            ).onSuccess {
                Timber.tag(TAG).e("Recover group wallet success")
                _uiState.update {
                    it.copy(
                        event = FreeGroupWalletRecoverEvent.RecoverSuccess(
                            walletName = _uiState.value.wallet?.name.orEmpty()
                        )
                    )
                }
            }.onFailure { error ->
                Timber.tag(TAG).e("Recover group wallet failed: $error")
                deleteWallet()
                _uiState.update { it.copy(errorMessage = error.readableMessage()) }
            }
        }
    }

    fun markMessageHandled() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    fun markEventHandled() {
        _uiState.update { it.copy(event = FreeGroupWalletRecoverEvent.None) }
    }

    fun showAddKeyErrorDialogHandled() {
        _uiState.update { it.copy(showAddKeyErrorDialog = false) }
    }

    fun setCurrentSignerIndex(index: Int) {
        Timber.tag(TAG).e("Set current signer index $index")
        savedStateHandle[CURRENT_SIGNER_INDEX] = index
    }

    fun setCurrentSignerKey(key: String) {
        Timber.tag(TAG).e("Set current signer key $key")
        savedStateHandle[CURRENT_SIGNER_KEY] = key
    }

    private fun deleteWallet() {
        viewModelScope.launch {
            deleteWalletUseCase(DeleteWalletUseCase.Params(walletId))
        }
    }

    fun getWallet() = _uiState.value.wallet
    fun updateWalletName(updatedWalletName: String) {
        _uiState.update { it.copy(wallet = _uiState.value.wallet?.copy(name = updatedWalletName)) }
    }

    private suspend fun getMiniscriptInfo() {
        val wallet = _uiState.value.wallet ?: return
        if (wallet.miniscript.isNotEmpty()) {
            getScriptNodeFromMiniscriptTemplateUseCase(wallet.miniscript).onSuccess { result ->
                _uiState.update { it.copy(scriptNode = result.scriptNode) }
                val signerMap = parseSignersFromScriptNode(result.scriptNode)
                _uiState.update { it.copy(signerMap = signerMap) }
                if (wallet.addressType == AddressType.TAPROOT && wallet.walletTemplate != WalletTemplate.DISABLE_KEY_PATH) {
                    if (wallet.totalRequireSigns > 1) {
                        val scriptNodeMuSig = MiniscriptUtil.buildMusigNode(wallet.totalRequireSigns)
                        // Create muSigSignerMap by mapping the first n signers to the scriptNodeMuSig keys
                        val muSigSignerMap = scriptNodeMuSig.keys.mapIndexed { index, key ->
                            key to wallet.signers.getOrNull(index)
                                ?.toModel()
                        }.toMap()

                        _uiState.update {
                            it.copy(
                                scriptNodeMuSig = scriptNodeMuSig,
                                muSigSignerMap = muSigSignerMap,
                            )
                        }
                    }
                }
            }.onFailure {
                Timber.tag(TAG).e("Failed to get miniscript info: ${it.message}")
            }
        } else {
            _uiState.update { it.copy(scriptNode = null) }
        }
    }

    private suspend fun parseSignersFromScriptNode(node: ScriptNode): Map<String, SignerModel?> {
        val signerMap = mutableMapOf<String, SignerModel?>()
        node.keys.forEach { key ->
            val signer = parseSignerStringUseCase(key).getOrNull()
           signer?.let {
               signerMap[key] =  singleSignerMapper(signer)
           }
        }
        node.subs.forEach { subNode ->
            signerMap.putAll(parseSignersFromScriptNode(subNode))
        }
        return signerMap
    }

    fun isMiniscriptWallet(): Boolean {
        return _uiState.value.wallet?.miniscript?.isNotEmpty() == true
    }

    companion object {
        private const val TAG = "recover-group-wallet"
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
        private const val CURRENT_SIGNER_KEY = "current_signer_key"
    }
}