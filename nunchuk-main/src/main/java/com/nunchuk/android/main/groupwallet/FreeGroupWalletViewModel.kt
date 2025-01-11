package com.nunchuk.android.main.groupwallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.free.groupwallet.GetGroupSandboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletViewModel @Inject constructor(
    private val getGroupSandboxUseCase: GetGroupSandboxUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val groupId =
        savedStateHandle.get<String>(FreeGroupWalletActivity.EXTRA_GROUP_ID).orEmpty()
    private val _uiState = MutableStateFlow(FreeGroupWalletUiState())
    val uiState: StateFlow<FreeGroupWalletUiState> = _uiState.asStateFlow()

    init {
        if (groupId.isEmpty()) {
            // init group
            // put group id to savedStateHandle after init
        } else {
            getGroupSandbox()
        }
    }

    private fun getGroupSandbox() {
        viewModelScope.launch {
            getGroupSandboxUseCase(groupId).onSuccess { groupSandbox ->
                _uiState.update { it.copy(group = groupSandbox) }
            }
        }
    }
}