package com.nunchuk.android.main.groupwallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.free.groupwallet.CreateGroupSandboxUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletViewModel @Inject constructor(
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val hasSignerUseCase: HasSignerUseCase,
    private val createGroupSandboxUseCase: CreateGroupSandboxUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val groupId =
        savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_GROUP_ID).orEmpty()
    private val _uiState = MutableStateFlow(FreeGroupWalletUiState())
    val uiState: StateFlow<FreeGroupWalletUiState> = _uiState.asStateFlow()

    init {
        if (groupId.isEmpty()) {
            createGroupSandbox()
        } else {
            getGroupSandbox()
        }
    }

    private fun createGroupSandbox() {
        viewModelScope.launch {
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
        }
    }

    private fun getGroupSandbox() {
        viewModelScope.launch {
            getGroupSandboxUseCase(groupId).onSuccess { groupSandbox ->
                updateGroupSandbox(groupSandbox)
            }
        }
    }

    private fun updateGroupSandbox(groupSandbox: GroupSandbox) {
        val signers = groupSandbox.signers.map {
            it.takeIf { it.masterFingerprint.isNotEmpty() }?.toModel()
        }
        _uiState.update { it.copy(group = groupSandbox, signers = signers) }
    }
}