package com.nunchuk.android.main.membership.key.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.signer.SignerModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSingerListBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = TapSignerListBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<TapSignerListBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private val _selectSingleId = MutableStateFlow("")
    val selectSingleId = _selectSingleId.asStateFlow()

    fun onSignerSelected(signer: SignerModel) {
        _selectSingleId.value = signer.id
    }

    fun onAddExistingKey() {
        viewModelScope.launch {
            selectedSigner?.let {
                _event.emit(TapSignerListBottomSheetEvent.OnAddExistingKey(it))
            }
        }
    }

    fun onAddNewKey() {
        viewModelScope.launch {
            _event.emit(TapSignerListBottomSheetEvent.OnAddNewKey)
        }
    }

    val selectedSigner: SignerModel?
        get() = args.signers.find { it.id == selectSingleId.value }
}

sealed class TapSignerListBottomSheetEvent {
    data class OnAddExistingKey(val signer: SignerModel) : TapSignerListBottomSheetEvent()
    object OnAddNewKey : TapSignerListBottomSheetEvent()
}