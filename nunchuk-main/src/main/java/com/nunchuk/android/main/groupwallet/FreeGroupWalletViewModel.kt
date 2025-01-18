package com.nunchuk.android.main.groupwallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.listener.GroupOnlineListener
import com.nunchuk.android.listener.GroupSandboxListener
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import com.nunchuk.android.usecase.free.groupwallet.CreateGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.DeleteGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupOnlineUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.RemoveSignerFromGroupUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
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
    private val pushEventManager: PushEventManager
) : ViewModel() {
    val groupId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_GROUP_ID).orEmpty()
    private val _uiState = MutableStateFlow(FreeGroupWalletUiState())
    val uiState: StateFlow<FreeGroupWalletUiState> = _uiState.asStateFlow()

    init {
        loadSigners()
        listenGroupSandbox()
        listenGroupOnline()
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
                if (groupSandbox.id == groupId) {
                    updateGroupSandbox(groupSandbox)
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
            createGroupSandboxUseCase(
                CreateGroupSandboxUseCase.Params(
                    "Group wallet",
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

    fun getGroupOnline() {
        viewModelScope.launch {
            getGroupOnlineUseCase(groupId).onSuccess { groupOnline ->
                _uiState.update { it.copy(numberOfOnlineUsers = groupOnline) }
            }
        }
    }

    fun getGroupSandbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getGroupSandboxUseCase(groupId).onSuccess { groupSandbox ->
                updateGroupSandbox(groupSandbox)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun updateGroupSandbox(groupSandbox: GroupSandbox) {
        val signers = groupSandbox.signers.map {
            it.takeIf { it.masterFingerprint.isNotEmpty() }?.let { signer ->
                getSignerUseCase(signer).getOrNull()?.toModel()?.copy(isVisible = true)
                    ?: it.toModel()
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
            }.onFailure {
                Timber.e("Failed to add signer to group $it")
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
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteGroupSandbox() {
        viewModelScope.launch {
            deleteGroupSandboxUseCase(groupId).onSuccess {
                _uiState.update { it.copy(isFinishScreen = true, group = null) }
            }
        }
    }

    companion object {
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
    }
}