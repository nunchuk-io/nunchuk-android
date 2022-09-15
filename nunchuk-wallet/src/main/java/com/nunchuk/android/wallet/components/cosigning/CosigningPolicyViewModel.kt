package com.nunchuk.android.wallet.components.cosigning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val args: CosigningPolicyFragmentArgs =
        CosigningPolicyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CosigningPolicyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(
        CosigningPolicyState(
            args.keyPolicy?.autoBroadcastTransaction ?: false,
            args.keyPolicy?.signingDelayInHour ?: 0
        )
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

    fun updateState(keyPolicy: KeyPolicy?) {
        keyPolicy ?: return
        _state.update {
            it.copy(
                isAutoBroadcast = keyPolicy.autoBroadcastTransaction,
                delayCosigningInHour = keyPolicy.signingDelayInHour
            )
        }
    }

    fun onEditClicked() {
        viewModelScope.launch {
            _event.emit(CosigningPolicyEvent.OnEditClicked)
        }
    }
}

data class CosigningPolicyState(val isAutoBroadcast: Boolean, val delayCosigningInHour: Int)

sealed class CosigningPolicyEvent {
    object OnEditClicked : CosigningPolicyEvent()
}