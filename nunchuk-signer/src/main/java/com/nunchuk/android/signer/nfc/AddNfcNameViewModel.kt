package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateTapSignerUseCase
import com.nunchuk.android.model.MasterSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNfcNameViewModel @Inject constructor(
    private val createTapSignerUseCase: CreateTapSignerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<AddNfcNameState?>(null)
    val state = _state.filterIsInstance<AddNfcNameState>()

    fun addNameForNfcKey(isoDep: IsoDep?, cvc: String, name: String) {
        isoDep ?: return
        _state.value = AddNfcNameState.Loading
        viewModelScope.launch {
            val result = createTapSignerUseCase(CreateTapSignerUseCase.Data(isoDep, cvc, name))
            if (result.isSuccess) {
                _state.value = AddNfcNameState.Success(result.getOrThrow())
            } else {
                _state.value = AddNfcNameState.Error(result.exceptionOrNull())
            }
        }
    }
}

sealed class AddNfcNameState {
    object Loading : AddNfcNameState()
    class Success(val masterSigner: MasterSigner) : AddNfcNameState()
    class Error(val e: Throwable?) : AddNfcNameState()
}