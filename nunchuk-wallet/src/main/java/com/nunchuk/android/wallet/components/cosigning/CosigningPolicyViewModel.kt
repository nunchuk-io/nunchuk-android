package com.nunchuk.android.wallet.components.cosigning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesUpdateKeyPolicyUseCase
import com.nunchuk.android.core.domain.membership.GetKeyPolicyUserDataUseCase
import com.nunchuk.android.core.domain.membership.UpdateServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.usecase.membership.GetServerKeysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CosigningPolicyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getServerKeysUseCase: GetServerKeysUseCase,
    private val updateServerKeysUseCase: UpdateServerKeysUseCase,
    private val calculateRequiredSignaturesUpdateKeyPolicyUseCase: CalculateRequiredSignaturesUpdateKeyPolicyUseCase,
    private val getKeyPolicyUserDataUseCase: GetKeyPolicyUserDataUseCase,
) : ViewModel() {
    private val args: CosigningPolicyFragmentArgs =
        CosigningPolicyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CosigningPolicyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(
        CosigningPolicyState(args.keyPolicy ?: KeyPolicy())
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = getServerKeysUseCase(args.xfp)
            if (result.isSuccess) {
                updateState(keyPolicy = result.getOrThrow())
            }
        }
    }

    fun updateState(keyPolicy: KeyPolicy?, isEditMode: Boolean = false) {
        keyPolicy ?: return
        _state.update {
            it.copy(
                keyPolicy = keyPolicy,
                isUpdateFlow = isEditMode
            )
        }
    }

    fun updateServerConfig(signatures: Map<String, String>) {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.Loading(true))
            val result = updateServerKeysUseCase(
                UpdateServerKeysUseCase.Param(
                    body = state.value.userData,
                    keyIdOrXfp = args.xfp,
                    signatures = signatures,
                    token = args.token
                )
            )
            _event.emit(CosigningPolicyEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(CosigningPolicyEvent.UpdateKeyPolicySuccess)
                _state.update { it.copy(isUpdateFlow = false) }
            } else {
                _event.emit(CosigningPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onDiscardChangeClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnDiscardChange)
        }
    }

    fun onSaveChangeClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.Loading(true))
            val result = calculateRequiredSignaturesUpdateKeyPolicyUseCase(
                CalculateRequiredSignaturesUpdateKeyPolicyUseCase.Param(
                    walletId = args.walletId,
                    keyPolicy = state.value.keyPolicy,
                    xfp = args.xfp
                )
            )
            _event.emit(CosigningPolicyEvent.Loading(false))
            if (result.isSuccess) {
                val data = getKeyPolicyUserDataUseCase(GetKeyPolicyUserDataUseCase.Param(
                    args.walletId,
                    state.value.keyPolicy
                )).getOrThrow()
                _state.update { it.copy(userData = data) }
                _event.emit(CosigningPolicyEvent.OnSaveChange(result.getOrThrow(), data))
            } else {
                _event.emit(CosigningPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onEditSpendingLimitClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnEditSpendingLimitClicked)
        }
    }

    fun onEditSigningDelayClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnEditSingingDelayClicked)
        }
    }
}

data class CosigningPolicyState(
    val keyPolicy: KeyPolicy = KeyPolicy(),
    val isUpdateFlow: Boolean = false,
    val userData: String = "",
    val signingDelayText: String = ""
)

sealed class CosigningPolicyEvent {
    class Loading(val isLoading: Boolean) : CosigningPolicyEvent()
    class ShowError(val error: String) : CosigningPolicyEvent()
    class OnSaveChange(val required: CalculateRequiredSignatures, val data: String) : CosigningPolicyEvent()
    object OnEditSpendingLimitClicked : CosigningPolicyEvent()
    object OnEditSingingDelayClicked : CosigningPolicyEvent()
    object OnDiscardChange : CosigningPolicyEvent()
    object UpdateKeyPolicySuccess : CosigningPolicyEvent()
}