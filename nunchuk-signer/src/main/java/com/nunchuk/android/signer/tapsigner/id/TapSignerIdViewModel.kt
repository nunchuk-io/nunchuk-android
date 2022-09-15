package com.nunchuk.android.signer.tapsigner.id

import android.nfc.tech.IsoDep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerIdViewModel @Inject constructor(
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args : TapSignerIdFragmentArgs = TapSignerIdFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<TapSignerIdEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            val result = getTapSignerStatusByIdUseCase(args.masterSignerId)
            if (result.isSuccess) {
                _state.update { it.copy(cardId = result.getOrThrow().ident.orEmpty()) }
            }
        }
    }

    private val _state = MutableStateFlow(TapSignerIdState(""))
    val state = _state.asStateFlow()


    fun getTapSignerBackup(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc, args.masterSignerId))
            _event.emit(TapSignerIdEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(TapSignerIdEvent.GetTapSignerBackupKeyError(result.exceptionOrNull()))
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.OnContinueClicked)
        }
    }

    fun onAddNewOneClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerIdEvent.OnAddNewOne)
        }
    }
}

data class TapSignerIdState(val cardId: String)

sealed class TapSignerIdEvent {
    object OnContinueClicked : TapSignerIdEvent()
    object OnAddNewOne : TapSignerIdEvent()
    data class NfcLoading(val isLoading: Boolean) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : TapSignerIdEvent()
    data class GetTapSignerBackupKeyError(val e: Throwable?) : TapSignerIdEvent()
}