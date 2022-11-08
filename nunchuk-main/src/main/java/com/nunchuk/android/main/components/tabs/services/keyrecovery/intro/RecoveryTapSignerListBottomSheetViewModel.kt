package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.SignerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryTapSignerListBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args =
        RecoveryTapSignerListBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<RecoveryTapSignerListBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(RecoveryTapSignerListBottomSheetState())
    val state = _state.asStateFlow()

    fun onSignerSelected(signer: SignerModel) = viewModelScope.launch {
        _state.update {
            it.copy(selectedSignerId = signer.id)
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        _event.emit(RecoveryTapSignerListBottomSheetEvent.ContinueClick)
    }

    val selectedSigner: SignerModel?
        get() = args.signers.find { it.id == _state.value.selectedSignerId }

}