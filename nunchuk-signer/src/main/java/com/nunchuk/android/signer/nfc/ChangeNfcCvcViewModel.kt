package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SetupTapSignerUseCase
import com.nunchuk.android.model.TapSignerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeNfcCvcViewModel @Inject constructor(
    private val setupTapSignerUseCase: SetupTapSignerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<ChangeNfcCvcState?>(null)
    val state = _state.filterIsInstance<ChangeNfcCvcState>()

    fun changeCvc(isoDep: IsoDep?, oldCvc: String, newCvc: String) {
        isoDep ?: return
        _state.value = ChangeNfcCvcState.Loading
        viewModelScope.launch {
            val result = setupTapSignerUseCase(SetupTapSignerUseCase.Data(isoDep, oldCvc, newCvc))
            if (result.isSuccess) {
                _state.value = ChangeNfcCvcState.Success(result.getOrThrow())
            } else {
                _state.value = ChangeNfcCvcState.Error(result.exceptionOrNull())
            }
        }
    }
}

sealed class ChangeNfcCvcState {
    object Loading : ChangeNfcCvcState()
    class Success(val backupString: String) : ChangeNfcCvcState()
    class Error(val e: Throwable?) : ChangeNfcCvcState()
}