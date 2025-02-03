package com.nunchuk.android.main.groupwallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.listener.GroupDeleteListener
import com.nunchuk.android.listener.GroupOnlineListener
import com.nunchuk.android.listener.GroupSandboxListener
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.CreateGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.DeleteGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupOnlineUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetPendingGroupsSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.RemoveSignerFromGroupUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSignerUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletViewModel @Inject constructor(
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val createGroupSandboxUseCase: CreateGroupSandboxUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val savedStateHandle: SavedStateHandle,
    private val addSignerToGroupUseCase: AddSignerToGroupUseCase,
    private val removeSignerFromGroupUseCase: RemoveSignerFromGroupUseCase,
    private val deleteGroupSandboxUseCase: DeleteGroupSandboxUseCase,
    private val getSignerUseCase: GetSignerUseCase,
    private val getGroupOnlineUseCase: GetGroupOnlineUseCase,
    private val pushEventManager: PushEventManager,
    private val getPendingGroupsSandboxUseCase: GetPendingGroupsSandboxUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
) : ViewModel() {
    val groupId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_GROUP_ID).orEmpty()
    private val _uiState = MutableStateFlow(FreeGroupWalletUiState())
    val uiState: StateFlow<FreeGroupWalletUiState> = _uiState.asStateFlow()

    init {
        loadSigners()
        listenGroupSandbox()
        listenGroupOnline()
        listenGroupDelete()
        if (groupId.isEmpty()) {
            createGroupSandbox()
        } else {
            getGroupSandbox()
            getGroupOnline()
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
        viewModelScope.launch {
            GroupSandboxListener.getGroupFlow().collect { groupSandbox ->
                Timber.d("GroupSandboxListener $groupSandbox")
                if (groupSandbox.id == groupId) {
                    if (groupSandbox.finalized) {
                        _uiState.update { it.copy(groupWalletUnavailable = true) }
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
                val singleSigner = pair.second.distinctBy { it.masterFingerprint }
                    .filter { it.type != SignerType.SERVER }
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
            val numOfGroups = getPendingGroupsSandboxUseCase(Unit).getOrNull()?.size ?: 0
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
            }.onFailure {
                Timber.e("Failed to create group sandbox $it")
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
                _uiState.update { it.copy(isLoading = true) }
                getGroupSandboxUseCase(groupId).onSuccess { groupSandbox ->
                    updateGroupSandbox(groupSandbox)
                }.onFailure {
                    Timber.e("Failed to get group sandbox $it")
                    if (it is NCNativeException && it.message.contains("-7008")) {
                        Timber.d("Group not found, finish screen")
                        _uiState.update { it.copy(isFinishScreen = true) }
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun updateGroupSandbox(groupSandbox: GroupSandbox) {
        val signers = groupSandbox.signers.map {

            it.takeIf { it.masterFingerprint.isNotEmpty() }?.let { signer ->
                if (hasSignerUseCase(signer).getOrNull() == true) {
                    getSignerUseCase(signer).getOrThrow().toModel().copy(isVisible = true)
                } else {
                    it.toModel().copy(isVisible = false)
                }
            }
        }
        _uiState.update { it.copy(group = groupSandbox, signers = signers) }
    }

    fun setCurrentSignerIndex(index: Int) {
        savedStateHandle[CURRENT_SIGNER_INDEX] = index
    }

    fun addSignerToGroup(signer: SingleSigner) {
        viewModelScope.launch {
            val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: return@launch
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
    }

    fun removeSignerFromGroup(index: Int) {
        viewModelScope.launch {
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
            state.supportedTypes.takeIf { state.group?.addressType?.isTaproot() == true }.orEmpty()
        }
    }

    companion object {
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
    }
}