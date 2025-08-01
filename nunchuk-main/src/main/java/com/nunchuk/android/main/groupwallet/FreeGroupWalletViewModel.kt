package com.nunchuk.android.main.groupwallet

import android.app.Application
import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetGroupDeviceUIDUseCase
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
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
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val setSlotOccupiedUseCase: SetSlotOccupiedUseCase,
    private val getGroupDeviceUIDUseCase: GetGroupDeviceUIDUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val createReplaceGroupUseCase: CreateReplaceGroupUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val finalizeGroupSandboxUseCase: FinalizeGroupSandboxUseCase,
    private val application: Application,
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
                    addSignerToGroup(it.signer)
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

    private fun createGroupSandbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val numOfGroups = getGroupWalletsUseCase(Unit).getOrNull()?.size ?: 0
            val groupName = if (numOfGroups == 0) {
                "Group wallet"
            } else {
                "Group wallet #${numOfGroups + 1}"
            }
            createGroupSandboxUseCase(
                CreateGroupSandboxUseCase.Params(
                    groupName,
                    2,
                    3,
                    AddressType.NATIVE_SEGWIT
                )
            ).onSuccess { groupSandbox ->
                savedStateHandle[FreeGroupWalletActivity.EXTRA_GROUP_ID] = groupSandbox.id
                updateGroupSandbox(groupSandbox)
            }.onFailure { error ->
                Timber.e("Failed to create group sandbox $error")
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
        Timber.d("Update group sandbox $groupSandbox")
        getSignerOldWallet(groupSandbox.replaceWalletId)
        val signers = mapSigners(groupSandbox.signers)
        if (replaceWalletId.isNotEmpty()) {
            _uiState.update { it.copy(group = groupSandbox, replaceSigners = signers, isInReplaceMode = true) }
        } else {
            _uiState.update { it.copy(group = groupSandbox, signers = signers, isInReplaceMode = false) }
        }
        updateOccupiedSlots(groupSandbox)
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
        val occupiedSlots =
            groupSandbox.occupiedSlots.mapIndexedNotNull { index, occupiedSlot ->
                if (occupiedSlot != null && occupiedSlot.deviceId != deviceUID && occupiedSlot.time + timeout > currentTimeInSeconds) {
                    index
                } else {
                    null
                }
            }.toSet()
        _uiState.update { it.copy(occupiedSlotsIndex = occupiedSlots) }
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
            val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: return@launch
            setSlotOccupiedUseCase(
                SetSlotOccupiedUseCase.Params(
                    groupId,
                    index,
                    value
                )
            ).onSuccess {
                Timber.d("Slot $index is occupied: $value")
            }.onFailure {
                Timber.e("Failed to set slot occupied $it")
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

    companion object {
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
        private const val CURRENT_SIGNER = "current_signer"
        private const val NEW_PATH = "new_path"
    }
}