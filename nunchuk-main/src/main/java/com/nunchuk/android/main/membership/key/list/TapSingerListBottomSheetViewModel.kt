package com.nunchuk.android.main.membership.key.list

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
) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerListBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private val _selectSingle = MutableStateFlow<SignerModel?>(null)
    val selectSingle = _selectSingle.asStateFlow()

    fun onSignerSelected(signer: SignerModel) {
        _selectSingle.value = signer
    }

    fun onAddExistingKey() {
        viewModelScope.launch {
            _selectSingle.value?.let {
                _event.emit(TapSignerListBottomSheetEvent.OnAddExistingKey(it))
            }
        }
    }

    fun onAddNewKey() {
        viewModelScope.launch {
            _event.emit(TapSignerListBottomSheetEvent.OnAddNewKey)
        }
    }
}

sealed class TapSignerListBottomSheetEvent {
    data class OnAddExistingKey(val signer: SignerModel) : TapSignerListBottomSheetEvent()
    object OnAddNewKey : TapSignerListBottomSheetEvent()
}